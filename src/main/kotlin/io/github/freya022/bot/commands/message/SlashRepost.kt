package io.github.freya022.bot.commands.message

import io.github.freya022.bot.commands.slash.SlashPost
import io.github.freya022.bot.link.TransformData
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.context.annotations.JDAMessageCommand
import io.github.freya022.botcommands.api.commands.application.context.message.GuildMessageEvent
import io.github.freya022.botcommands.api.core.entities.asInputUser

@Command
class MessageRepost(private val slashPost: SlashPost) : ApplicationCommand() {
    @JDAMessageCommand(name = "Repost")
    suspend fun onMessageRepost(event: GuildMessageEvent) {
        slashPost.postWithTransform(event, event.target.member?.asInputUser() ?: event.target.author.asInputUser(), TransformData(event.target))
    }
}