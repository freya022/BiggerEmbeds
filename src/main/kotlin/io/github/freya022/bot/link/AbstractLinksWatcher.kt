package io.github.freya022.bot.link

import club.minnced.discord.webhook.send.WebhookMessage
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import dev.minn.jda.ktx.coroutines.await
import io.github.freya022.bot.WebhookStore
import io.github.freya022.botcommands.api.core.annotations.BEventListener
import io.github.freya022.botcommands.api.core.service.annotations.InterfacedService
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent

private val logger = KotlinLogging.logger { }

@InterfacedService(acceptMultiple = true)
abstract class AbstractLinksWatcher(private val webhookStore: WebhookStore) {
    //discord-webhook likes to read the entirety of the file, 1KB per 1KB
    // so this hack is actually required if you don't want to allocate 229 GB for a 21 MB file
    class EditableMessage private constructor(
        val webhookMessageBuilder: WebhookMessageBuilder,
        var content: String,
        val files: MutableMap<String, ByteArray>
    ) {
        companion object {
            suspend fun fromMessage(message: Message): EditableMessage {
                val attachments: MutableMap<String, ByteArray> = hashMapOf()
                message.attachments.forEach {
                    attachments[it.fileName] = it.proxy.download().await().readAllBytes()
                }
                val messageBuilder = runCatching { WebhookMessageBuilder.fromJDA(message) }
                    .onFailure { logger.trace("Error while constructing an EditableMessage", it) }
                    .getOrElse { WebhookMessageBuilder() }
                return EditableMessage(messageBuilder, message.contentRaw, attachments)
            }

            fun fromContent(content: String): EditableMessage {
                return EditableMessage(WebhookMessageBuilder().setContent(content), content, hashMapOf())
            }
        }
    }

    private val map: MutableMap<SourceMessageID, RepostedMessageID> = hashMapOf()

    @BEventListener
    suspend fun onMessage(event: MessageReceivedEvent) {
        if (event.author.isBot || event.isWebhookMessage) return

        val channel = event.channel
        if (channel !is IWebhookContainer) return

        checkLinks(event.member!!, event.message) {
            val repostedMessageId = webhookStore.getWebhook(channel).send(it).await().id
            if (event.guild.selfMember.hasPermission(Permission.MESSAGE_MANAGE)) {
                event.message.suppressEmbeds(true).await()
            }
            map[event.messageIdLong] = repostedMessageId
        }
    }

    @BEventListener
    suspend fun onEdit(event: MessageUpdateEvent) {
        val repostedId = map[event.messageIdLong] ?: return

        checkLinks(event.member!!, event.message) {
            webhookStore.getWebhook(event.channel as IWebhookContainer).edit(repostedId, it).await()
        }
    }

    private suspend fun checkLinks(member: Member, message: Message, block: suspend (WebhookMessage) -> Unit) {
        editMessageIfNeededOrNull(message)
            ?.let { createMessage(member, it) }
            ?.let { block(it) }
    }

    private suspend fun editMessageIfNeededOrNull(message: Message): EditableMessage? {
        return editMessageIfNeededOrNull(EditableMessage.fromMessage(message))
    }

    abstract fun editMessageIfNeededOrNull(editableMessage: EditableMessage): EditableMessage?

    companion object {
        fun createMessage(
            member: Member,
            editableMessage: EditableMessage
        ): WebhookMessage {
            return editableMessage.webhookMessageBuilder
                .setUsername(member.effectiveName)
                .setAvatarUrl(member.getEffectiveAvatarUrl())
                .setContent(editableMessage.content)
                .also { builder ->
                    editableMessage.files.forEach { (name, bytes) -> builder.addFile(name, bytes) }
                }
                .build()
        }
    }
}