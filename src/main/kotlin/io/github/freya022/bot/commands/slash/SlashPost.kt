package io.github.freya022.bot.commands.slash

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.bot.WebhookStore
import io.github.freya022.bot.link.MessageTransformer
import io.github.freya022.bot.link.TransformData
import io.github.freya022.bot.utils.runDynamicHook
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData
import io.github.freya022.botcommands.api.core.entities.inputUser
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.interactions.IntegrationType.GUILD_INSTALL
import net.dv8tion.jda.api.interactions.IntegrationType.USER_INSTALL
import net.dv8tion.jda.api.interactions.InteractionContextType.GUILD
import net.dv8tion.jda.api.interactions.InteractionContextType.PRIVATE_CHANNEL
import net.dv8tion.jda.api.utils.messages.MessageCreateData

@Command
class SlashPost(
    private val webhookStore: WebhookStore,
    private val messageTransformers: List<MessageTransformer>,
) : ApplicationCommand() {
    @JDASlashCommand(name = "post", description = "Send media with better embeds")
    @TopLevelSlashCommandData(
        contexts = [GUILD, PRIVATE_CHANNEL],
        integrationTypes = [GUILD_INSTALL, USER_INSTALL],
    )
    suspend fun onSlashPost(event: GlobalSlashEvent, @SlashOption(description = "The post content") post: String) {
        suspend fun tryTransformMessage(ephemeral: Boolean, block: suspend (MessageCreateData) -> Unit) {
            val message = runDynamicHook(event, ephemeral) {
                val data = TransformData(post)
                messageTransformers.forEach { it.processMessage(data) }
                data.buildMessageOrNull()
            }

            requireNotNull(message) {
                "Message can only be valid"
            }

            block(message)
        }

        val guild = event.guild
        if (guild?.isDetached == false) {
            val channel = event.channel
            if (channel !is IWebhookContainer)
                return event.reply_("Can only run in channels with webhooks", ephemeral = true).queue()
            if (!guild.selfMember.hasPermission(channel, Permission.MANAGE_WEBHOOKS))
                return event.reply_("I require the permission to manage webhooks", ephemeral = true).queue()

            tryTransformMessage(ephemeral = true) { message ->
                // If not yet thinking, think
                if (!event.isAcknowledged) {
                    event.deferReply(true).queue()
                }

                val fakedUser = event.inputUser
                webhookStore.sendMessage(channel) { webhook ->
                    webhook.sendMessage(message)
                        .setUsername(fakedUser.effectiveName)
                        .setAvatarUrl(fakedUser.effectiveAvatarUrl)
                }

                event.hook.deleteOriginal().queue()
            }
        } else {
            // Detached guilds
            tryTransformMessage(ephemeral = false) { message ->
                if (event.isAcknowledged) {
                    event.hook.sendMessage(message).await()
                } else {
                    event.reply(message).await()
                }
            }
        }
    }
}