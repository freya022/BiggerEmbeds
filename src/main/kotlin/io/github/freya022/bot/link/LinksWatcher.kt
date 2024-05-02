package io.github.freya022.bot.link

import dev.minn.jda.ktx.coroutines.await
import io.github.freya022.bot.WebhookStore
import io.github.freya022.botcommands.api.core.annotations.BEventListener
import io.github.freya022.botcommands.api.core.service.annotations.BService
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

@BService
class LinksWatcher(
    private val webhookStore: WebhookStore,
    private val linkTransformers: List<LinkTransformer>
) {
    @BEventListener
    suspend fun onMessage(event: MessageReceivedEvent) {
        if (event.author.isBot || event.isWebhookMessage || event.message.type.isSystem) return

        val channel = event.channel
        if (channel !is IWebhookContainer) return

        val data = TransformData(event.message)
        linkTransformers.forEach { it.processMessage(data) }
        if (!data.hasChanged) return

        webhookStore.getWebhook(channel)
            .sendMessage(data.buildMessage())
            .setUsername(event.member!!.effectiveName)
            .setAvatarUrl(event.member!!.effectiveAvatarUrl)
            .await()

        if (event.guild.selfMember.hasPermission(Permission.MESSAGE_MANAGE)) {
            event.message.suppressEmbeds(true).await()
        }
    }
}