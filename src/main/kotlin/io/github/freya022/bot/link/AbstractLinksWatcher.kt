package io.github.freya022.bot.link

import dev.minn.jda.ktx.coroutines.await
import io.github.freya022.bot.WebhookStore
import io.github.freya022.botcommands.api.core.annotations.BEventListener
import io.github.freya022.botcommands.api.core.service.annotations.InterfacedService
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder
import net.dv8tion.jda.api.utils.messages.MessageRequest

@InterfacedService(acceptMultiple = true)
abstract class AbstractLinksWatcher(private val webhookStore: WebhookStore) {
    private val map: MutableMap<SourceMessageID, RepostedMessageID> = hashMapOf()

    @BEventListener
    suspend fun onMessage(event: MessageReceivedEvent) {
        if (event.author.isBot || event.isWebhookMessage) return

        val channel = event.channel
        if (channel !is IWebhookContainer) return

        val member = event.member!!
        checkLinks(event.message, MessageCreateBuilder()) {
            val repostedMessageId = webhookStore.getWebhook(channel).sendMessage(it.build())
                .setUsername(member.effectiveName)
                .setAvatarUrl(member.effectiveAvatarUrl)
                .await().idLong
            if (event.guild.selfMember.hasPermission(Permission.MESSAGE_MANAGE)) {
                event.message.suppressEmbeds(true).await()
            }
            map[event.messageIdLong] = repostedMessageId
        }
    }

    @BEventListener
    suspend fun onEdit(event: MessageUpdateEvent) {
        val repostedId = map[event.messageIdLong] ?: return

        checkLinks(event.message, MessageEditBuilder()) {
            webhookStore.getWebhook(event.channel as IWebhookContainer).editMessageById(repostedId, it.build()).await()
        }
    }

    private suspend fun <T : MessageRequest<*>> checkLinks(message: Message, blankBuilder: T, block: suspend (T) -> Unit) {
        editMessageIfNeededOrNull(message, blankBuilder)?.let { block(it) }
    }

    private suspend fun <T : MessageRequest<*>> editMessageIfNeededOrNull(message: Message, blankBuilder: T): T? {
        blankBuilder.applyMessage(message)
        val attachments = message.attachments.map { FileUpload.fromData(it.proxy.download().await(), it.fileName) }
        blankBuilder.setFiles(attachments)

        return editMessageIfNeededOrNull(blankBuilder, attachments)
    }

    abstract fun <T : MessageRequest<*>> editMessageIfNeededOrNull(builder: T, attachments: List<FileUpload>): T?
}