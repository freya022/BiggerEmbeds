package io.github.freya022.bot.video

import dev.freya02.botcommands.jda.ktx.coroutines.await
import dev.freya02.botcommands.jda.ktx.requests.runIgnoringResponse
import dev.freya02.jda.emojis.unicode.Emojis
import io.github.freya022.bot.config.Config
import io.github.freya022.bot.utils.Size.Companion.kilobits
import io.github.freya022.botcommands.api.core.annotations.BEventListener
import io.github.freya022.botcommands.api.core.service.annotations.BService
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.GatewayIntent

@BService
class HighBitrateVideoListener(
    config: Config,
    private val controller: HighBitrateVideoController,
) {

    private val guildIds = config.videoListenerGuildIds

    @BEventListener(ignoredIntents = [GatewayIntent.DIRECT_MESSAGES])
    suspend fun onMessage(event: MessageReceivedEvent) {
        if (!event.isFromGuild) return
        if (event.guild.idLong !in guildIds) return
        if (event.author.isBot || event.isWebhookMessage || event.message.type.isSystem) return

        if (!event.guild.selfMember.hasPermission(event.guildChannel, Permission.MESSAGE_ADD_REACTION))
            return

        val hasHeavyVideos = event.message.attachments.filter { it.isVideo }.any {
            controller.getClipStats(it.proxyUrl).bitrate >= 10000.kilobits
        }

        // Check again as quite a "long" time might have passed
        if (hasHeavyVideos && event.guild.selfMember.hasPermission(event.guildChannel, Permission.MESSAGE_ADD_REACTION)) {
            runIgnoringResponse(ErrorResponse.MISSING_PERMISSIONS) {
                event.message.addReaction(Emojis.BRICKS).await()
            }
        }
    }
}
