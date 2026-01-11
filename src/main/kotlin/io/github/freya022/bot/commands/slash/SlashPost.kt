package io.github.freya022.bot.commands.slash

import dev.freya02.botcommands.jda.ktx.coroutines.await
import dev.freya02.botcommands.jda.ktx.messages.toEditData
import gnu.trove.set.hash.TLongHashSet
import io.github.freya022.bot.WebhookStore
import io.github.freya022.bot.link.MessageTransformer
import io.github.freya022.bot.link.TransformData
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData
import net.dv8tion.jda.api.Permission.MANAGE_ROLES
import net.dv8tion.jda.api.Permission.MANAGE_WEBHOOKS
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

    private val webhookPermissionNotifiedUsers = TLongHashSet()

    @JDASlashCommand(name = "post", description = "Send media with better embeds")
    @TopLevelSlashCommandData(
        contexts = [GUILD, PRIVATE_CHANNEL],
        integrationTypes = [GUILD_INSTALL, USER_INSTALL],
    )
    suspend fun onSlashPost(event: GlobalSlashEvent, @SlashOption(description = "The post content") post: String) {
        val guild = event.guild
        val channel = event.channel
        val isAttachedGuild = guild != null && !guild.isDetached

        if (isAttachedGuild && channel is IWebhookContainer) {
            val member = event.member!!
            val hasWebhookPermissions = guild.selfMember.hasPermission(channel, MANAGE_WEBHOOKS)

            event.deferReply()
                .setEphemeral(hasWebhookPermissions)
                .queue()

            val message = transformMessage(post)

            if (hasWebhookPermissions) {
                webhookPermissionNotifiedUsers.remove(member.idLong)

                webhookStore.sendMessage(channel) { webhook ->
                    webhook.sendMessage(message)
                        .setUsername(member.effectiveName)
                        .setAvatarUrl(member.effectiveAvatarUrl)
                }

                event.hook.deleteOriginal().queue()
            } else {
                event.hook.editOriginal(message.toEditData()).await()

                // We didn't have webhook permissions and the caller has them, ask them to give us once
                if (member.hasPermission(MANAGE_WEBHOOKS, MANAGE_ROLES) && member.canInteract(guild.selfMember) && webhookPermissionNotifiedUsers.add(member.idLong)) {
                    event.hook
                        .sendMessage("**Note:** Enable better replies by giving me the permission to manage webhooks!")
                        .setEphemeral(true)
                        .queue()
                }
            }
        } else {
            // Detached guilds, DMs
            event.deferReply(false).queue()

            val message = transformMessage(post)
            event.hook.editOriginal(message.toEditData()).queue()
        }
    }

    private suspend fun transformMessage(post: String): MessageCreateData {
        val message = run {
            val data = TransformData(post)
            messageTransformers.forEach { it.processMessage(data) }
            data.buildMessageOrNull()
        }

        requireNotNull(message) {
            "Message can only be valid"
        }

        return message
    }
}
