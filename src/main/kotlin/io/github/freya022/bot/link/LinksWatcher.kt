package io.github.freya022.bot.link

import dev.freya02.botcommands.jda.ktx.coroutines.await
import io.github.freya022.bot.WebhookStore
import io.github.freya022.botcommands.api.core.annotations.BEventListener
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

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
            return logger.debug { "No webhook perms in ${channel.name} (${channel.id})" }

        val message = MessageTransformers.transformMessage(
            MessageCreateBuilder.fromMessage(event.message),
            event.message.attachments,
            messageTransformers
        ) ?: return

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
