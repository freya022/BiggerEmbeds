package io.github.freya022.bot.commands.message

import dev.freya02.botcommands.jda.ktx.messages.reply_
import io.github.freya022.bot.commands.CommonPost
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.context.annotations.JDAMessageCommand
import io.github.freya022.botcommands.api.commands.application.context.message.GlobalMessageEvent
import net.dv8tion.jda.api.interactions.IntegrationType.GUILD_INSTALL
import net.dv8tion.jda.api.interactions.IntegrationType.USER_INSTALL
import net.dv8tion.jda.api.interactions.InteractionContextType.GUILD
import net.dv8tion.jda.api.interactions.InteractionContextType.PRIVATE_CHANNEL
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

@Command
class MessageRepost(
    private val commonPost: CommonPost,
) {

    @JDAMessageCommand(
        name = "Repost",
        contexts = [GUILD, PRIVATE_CHANNEL],
        integrationTypes = [GUILD_INSTALL, USER_INSTALL],
    )
    suspend fun onMessageRepost(event: GlobalMessageEvent) {
        val message = event.target
        if (message.author.isBot || message.isWebhookMessage)
            return event.reply_("Cannot repost a bot message", ephemeral = true).queue()
        if (message.type.isSystem)
            return event.reply_("Cannot repost a system message", ephemeral = true).queue()
        if (message.isVoiceMessage)
            return event.reply_("Cannot repost a voice message", ephemeral = true).queue()

        commonPost.sendPost(event, baseBuilderSupplier = { MessageCreateBuilder.fromMessage(message) })
    }
}
