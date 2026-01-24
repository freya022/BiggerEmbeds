package io.github.freya022.bot.commands

import dev.freya02.botcommands.jda.ktx.coroutines.await
import dev.freya02.botcommands.jda.ktx.messages.toEditData
import gnu.trove.set.hash.TLongHashSet
import io.github.freya022.bot.WebhookStore
import io.github.freya022.bot.link.MessageTransformer
import io.github.freya022.bot.link.MessageTransformers
import io.github.freya022.botcommands.api.core.service.annotations.BService
import net.dv8tion.jda.api.Permission.MANAGE_ROLES
import net.dv8tion.jda.api.Permission.MANAGE_WEBHOOKS
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData

@BService
class CommonPost(
    private val webhookStore: WebhookStore,
    private val messageTransformers: List<MessageTransformer>,
) {
    private val webhookPermissionNotifiedUsers = TLongHashSet()

    suspend fun sendPost(interaction: IReplyCallback, baseBuilderSupplier: () -> MessageCreateBuilder) {
        val guild = interaction.guild
        val channel = interaction.channel
        val isAttachedGuild = guild != null && !guild.isDetached

        if (isAttachedGuild && channel is IWebhookContainer) {
            val member = interaction.member!!
            val hasWebhookPermissions = guild.selfMember.hasPermission(channel, MANAGE_WEBHOOKS)

            interaction.deferReply()
                .setEphemeral(hasWebhookPermissions)
                .queue()

            val message = transformMessage(baseBuilderSupplier)

            if (hasWebhookPermissions) {
                webhookPermissionNotifiedUsers.remove(member.idLong)

                webhookStore.sendMessage(channel) { webhook ->
                    webhook.sendMessage(message)
                        .setUsername(member.effectiveName)
                        .setAvatarUrl(member.effectiveAvatarUrl)
                }

                interaction.hook.deleteOriginal().queue()
            } else {
                interaction.hook.editOriginal(message.toEditData()).await()

                // We didn't have webhook permissions and the caller has them, ask them to give us once
                if (member.hasPermission(MANAGE_WEBHOOKS, MANAGE_ROLES) && member.canInteract(guild.selfMember) && webhookPermissionNotifiedUsers.add(member.idLong)) {
                    interaction.hook
                        .sendMessage("**Note:** Enable better replies by giving me the permission to manage webhooks!")
                        .setEphemeral(true)
                        .queue()
                }
            }
        } else {
            // Detached guilds, DMs
            interaction.deferReply(false).queue()

            val message = transformMessage(baseBuilderSupplier)
            interaction.hook.editOriginal(message.toEditData()).queue()
        }
    }

    private suspend fun transformMessage(baseBuilderSupplier: () -> MessageCreateBuilder): MessageCreateData {
        return MessageTransformers.transformMessage(baseBuilderSupplier(), emptyList(), messageTransformers)
            ?: baseBuilderSupplier().build()
    }
}
