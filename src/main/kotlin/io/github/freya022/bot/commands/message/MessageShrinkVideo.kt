package io.github.freya022.bot.commands.message

import dev.freya02.botcommands.jda.ktx.coroutines.await
import dev.freya02.botcommands.jda.ktx.messages.MessageCreate
import dev.freya02.botcommands.jda.ktx.messages.reply_
import dev.freya02.botcommands.jda.ktx.messages.toEditData
import io.github.freya022.bot.WebhookStore
import io.github.freya022.bot.config.Config
import io.github.freya022.bot.video.HighBitrateVideoController
import io.github.freya022.bot.video.HighBitrateVideoController.PathUpload
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.context.message.GuildMessageEvent
import io.github.freya022.botcommands.api.commands.application.provider.GuildApplicationCommandManager
import io.github.freya022.botcommands.api.commands.application.provider.GuildApplicationCommandProvider
import io.github.freya022.botcommands.api.core.service.getService
import net.dv8tion.jda.api.Permission.MANAGE_WEBHOOKS
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType

@Command
class MessageShrinkVideo(
    private val controller: HighBitrateVideoController,
    private val webhookStore: WebhookStore,
) : GuildApplicationCommandProvider {

    suspend fun onMessageShrinkVideo(event: GuildMessageEvent) {
        val message = event.target
        if (message.author.isBot || message.isWebhookMessage)
            return event.reply_("Cannot process a bot message", ephemeral = true).queue()
        if (message.type.isSystem)
            return event.reply_("Cannot process a system message", ephemeral = true).queue()
        if (message.isVoiceMessage)
            return event.reply_("Cannot process a voice message", ephemeral = true).queue()

        val guild = event.guild
        val channel = event.channel
        val webhookContainer = when (channel) {
            is IWebhookContainer -> channel
            is ThreadChannel -> channel.parentChannel as? IWebhookContainer
            else -> null
        }

        if (webhookContainer != null) {
            val member = event.member
            val hasWebhookPermissions = guild.selfMember.hasPermission(webhookContainer, MANAGE_WEBHOOKS)

            event.deferReply()
                .setEphemeral(hasWebhookPermissions)
                .queue()

            (val newAttachments, val galleryItems = items) = controller.tryShrinkVideos(message) ?: return

            try {
                val messageData = MessageCreate(useComponentsV2 = true) { mediaGallery(galleryItems) }
                if (hasWebhookPermissions) {
                    webhookStore.sendMessage(webhookContainer) { webhook ->
                        webhook.sendMessage(messageData)
                            .also {
                                if (channel is ThreadChannel) {
                                    it.setThread(channel)
                                }
                            }
                            .setUsername(member.effectiveName)
                            .setAvatarUrl(member.effectiveAvatarUrl)
                    }

                    event.hook.deleteOriginal().queue()
                } else {
                    event.hook.editOriginal(messageData.toEditData()).await()
                }
            } finally {
                newAttachments.forEach(PathUpload::close)
            }
        } else {
            // Who knows where this is
            event.deferReply(false).queue()

            (val newAttachments, val galleryItems = items) = controller.tryShrinkVideos(message) ?: return

            try {
                val messageData = MessageCreate(useComponentsV2 = true) { mediaGallery(galleryItems) }
                event.hook.editOriginal(messageData.toEditData()).await()
            } finally {
                newAttachments.forEach(PathUpload::close)
            }
        }
    }

    override fun declareGuildApplicationCommands(manager: GuildApplicationCommandManager) {
        val videoListenerGuildIds = manager.context.getService<Config>().videoListenerGuildIds
        if (manager.guild.idLong !in videoListenerGuildIds) return

        manager.messageCommand("Shrink video", ::onMessageShrinkVideo) {
            integrationTypes = setOf(IntegrationType.GUILD_INSTALL)
            contexts = setOf(InteractionContextType.GUILD)
        }
    }
}
