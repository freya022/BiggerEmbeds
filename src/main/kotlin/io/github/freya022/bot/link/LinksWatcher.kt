package io.github.freya022.bot.link

import dev.minn.jda.ktx.coroutines.await
import io.github.freya022.bot.WebhookStore
import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.api.core.annotations.BEventListener
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.service.getInterfacedServices
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

@BService
class LinksWatcher(context: BContext, private val webhookStore: WebhookStore) {
    private val linkTransformers = context.getInterfacedServices<LinkTransformer>()

    @BEventListener
    suspend fun onMessage(event: MessageReceivedEvent) {
        if (event.author.isBot || event.isWebhookMessage || event.message.type.isSystem) return

        val channel = event.channel
        if (channel !is IWebhookContainer) return

        var changed = false
        var builder = MessageCreateBuilder.fromMessage(event.message)
        for (linkWatcher in linkTransformers) {
            val newData = linkWatcher.editMessageIfNeededOrNull(builder)
            if (newData != null) {
                changed = true
                builder = newData
            }
        }
        if (!changed) {
            return
        }

        val messageAttachments = event.message.attachments.map { FileUpload.fromData(it.proxy.download().await(), it.fileName) }
        builder.setFiles(messageAttachments + builder.attachments)

        webhookStore.getWebhook(channel)
            .sendMessage(builder.build())
            .setUsername(event.member!!.effectiveName)
            .setAvatarUrl(event.member!!.effectiveAvatarUrl)
            .await()

        if (event.guild.selfMember.hasPermission(Permission.MESSAGE_MANAGE)) {
            event.message.suppressEmbeds(true).await()
        }
    }
}