package io.github.freya022.bot.link

import dev.minn.jda.ktx.coroutines.await
import io.github.freya022.bot.WebhookStore
import io.github.freya022.botcommands.api.core.annotations.BEventListener
import io.github.freya022.botcommands.api.core.service.annotations.InterfacedService
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

@InterfacedService(acceptMultiple = true)
abstract class AbstractLinksWatcher(private val webhookStore: WebhookStore) {
    private val map: MutableMap<SourceMessageID, RepostedMessageID> = hashMapOf()

    @BEventListener
    suspend fun onMessage(event: MessageReceivedEvent) {
        if (event.author.isBot || event.isWebhookMessage) return

        val channel = event.channel
        if (channel !is IWebhookContainer) return

        val member = event.member!!
        checkLinks(event.message, MessageCreateBuilder()) { builder ->
            val messageAttachments = event.message.attachments.map { FileUpload.fromData(it.proxy.download().await(), it.fileName) }
            builder.setFiles(messageAttachments + builder.attachments) //Put existing attachments before added ones

            val repostedMessageId = webhookStore.getWebhook(channel).sendMessage(builder.build())
                .setUsername(member.effectiveName)
                .setAvatarUrl(member.effectiveAvatarUrl)
                .await().idLong
            if (event.guild.selfMember.hasPermission(Permission.MESSAGE_MANAGE)) {
                event.message.suppressEmbeds(true).await()
            }
            map[event.messageIdLong] = repostedMessageId
        }
    }

    private suspend fun checkLinks(message: Message, blankBuilder: MessageCreateBuilder, block: suspend (MessageCreateBuilder) -> Unit) {
        editMessageIfNeededOrNull(message, blankBuilder)?.let { block(it) }
    }

    private fun editMessageIfNeededOrNull(message: Message, blankBuilder: MessageCreateBuilder): MessageCreateBuilder? {
        blankBuilder.applyMessage(message)
        return editMessageIfNeededOrNull(blankBuilder)
    }

    abstract fun editMessageIfNeededOrNull(builder: MessageCreateBuilder): MessageCreateBuilder?
}