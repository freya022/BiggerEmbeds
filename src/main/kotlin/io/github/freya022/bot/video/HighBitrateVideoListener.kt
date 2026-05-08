package io.github.freya022.bot.video

import dev.freya02.botcommands.jda.ktx.components.MediaGallery
import io.github.freya022.bot.WebhookStore
import io.github.freya022.bot.config.Config
import io.github.freya022.bot.video.HighBitrateVideoController.PathUpload
import io.github.freya022.botcommands.api.core.annotations.BEventListener
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.GatewayIntent

private val logger = KotlinLogging.logger { }

@BService
class HighBitrateVideoListener(
    config: Config,
    private val controller: HighBitrateVideoController,
    private val webhookStore: WebhookStore,
) {

    private val guildIds = config.videoListenerGuildIds

    @BEventListener(ignoredIntents = [GatewayIntent.DIRECT_MESSAGES])
    suspend fun onMessage(event: MessageReceivedEvent) {
        if (!event.isFromGuild) return
        if (event.guild.idLong !in guildIds) return
        if (event.author.isBot || event.isWebhookMessage || event.message.type.isSystem) return

        val channel = event.channel
        if (channel !is IWebhookContainer) return
        if (!event.guild.selfMember.hasPermission(channel, Permission.MANAGE_WEBHOOKS))
            return logger.debug { "No webhook perms in ${channel.name} (${channel.id})" }

        (val newAttachments, val galleryItems = items) = controller.tryShrinkVideos(event.message)
            ?: return

        try {
            webhookStore.sendMessage(channel) { webhook ->
                val components = listOf(
                    MediaGallery {
                        items += galleryItems
                    },
                )
                webhook.sendMessageComponents(components)
                    .useComponentsV2()
                    .setUsername(event.member!!.effectiveName)
                    .setAvatarUrl(event.member!!.effectiveAvatarUrl)
            }
        } finally {
            newAttachments.forEach(PathUpload::close)
        }
    }
}
