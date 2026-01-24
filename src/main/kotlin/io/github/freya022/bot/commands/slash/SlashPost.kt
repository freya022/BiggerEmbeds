package io.github.freya022.bot.commands.slash

import io.github.freya022.bot.commands.CommonPost
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData
import net.dv8tion.jda.api.interactions.IntegrationType.GUILD_INSTALL
import net.dv8tion.jda.api.interactions.IntegrationType.USER_INSTALL
import net.dv8tion.jda.api.interactions.InteractionContextType.GUILD
import net.dv8tion.jda.api.interactions.InteractionContextType.PRIVATE_CHANNEL
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

@Command
class SlashPost(
    private val commonPost: CommonPost,
) : ApplicationCommand() {

    @JDASlashCommand(name = "post", description = "Send media with better embeds")
    @TopLevelSlashCommandData(
        contexts = [GUILD, PRIVATE_CHANNEL],
        integrationTypes = [GUILD_INSTALL, USER_INSTALL],
    )
    suspend fun onSlashPost(event: GlobalSlashEvent, @SlashOption(description = "The post content") post: String) {
        commonPost.sendPost(event, baseBuilderSupplier = { MessageCreateBuilder().setContent(post) })
    }
}
