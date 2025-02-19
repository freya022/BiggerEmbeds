package io.github.freya022.bot.link

import dev.minn.jda.ktx.coroutines.await
import io.github.freya022.bot.WebhookStore
import io.github.freya022.botcommands.api.core.annotations.BEventListener
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.GatewayIntent

private val logger = KotlinLogging.logger { }

@BService
class LinksWatcher(
    private val webhookStore: WebhookStore,
    private val messageTransformers: List<MessageTransformer>
) {

    @BEventListener(ignoredIntents = [GatewayIntent.DIRECT_MESSAGES])
    suspend fun onMessage(event: MessageReceivedEvent) {
        if (event.author.isBot || event.isWebhookMessage || event.message.type.isSystem) return

        val channel = event.channel
        if (channel !is IWebhookContainer) return
        if (!event.guild.selfMember.hasPermission(channel, Permission.MANAGE_WEBHOOKS))
            return logger.debug { "No debug perms in ${channel.name} (${channel.id})" }

        val data = TransformData(event.message)
        messageTransformers.forEach { it.processMessage(data) }
        if (!data.hasChanged) return

        val message = data.buildMessageOrNull()
            ?: return logger.error { "How did we get an invalid message? ID: ${event.messageId}" }
        webhookStore.sendMessage(channel) { webhook ->
            webhook.sendMessage(message)
                .setUsername(event.member!!.effectiveName)
                .setAvatarUrl(event.member!!.effectiveAvatarUrl)
        }

        if (event.guild.selfMember.hasPermission(Permission.MESSAGE_MANAGE)) {
            event.message.suppressEmbeds(true).await()
        }
    }
}