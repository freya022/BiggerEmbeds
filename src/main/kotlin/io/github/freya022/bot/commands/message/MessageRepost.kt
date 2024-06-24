package io.github.freya022.bot.commands.message

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.bot.WebhookStore
import io.github.freya022.bot.link.MessageTransformer
import io.github.freya022.bot.link.TransformData
import io.github.freya022.bot.utils.runDynamicHook
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.context.annotations.JDAMessageCommand
import io.github.freya022.botcommands.api.commands.application.context.message.GuildMessageEvent
import io.github.freya022.botcommands.api.core.entities.inputUser
import io.github.freya022.botcommands.api.core.utils.deleteDelayed
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import kotlin.time.Duration.Companion.seconds

@Command
class MessageRepost(
    private val webhookStore: WebhookStore,
    private val messageTransformers: List<MessageTransformer>,
) : ApplicationCommand() {
    @JDAMessageCommand(name = "Repost")
    suspend fun onMessageRepost(event: GuildMessageEvent) {
        suspend fun sendNoContentMessage() {
            if (event.isAcknowledged) {
                event.hook.sendMessage("No content to post")
                    .deleteDelayed(5.seconds)
                    .await()
            } else {
                event.reply_("No content to post", ephemeral = true)
                    .deleteDelayed(5.seconds)
                    .await()
            }
        }

        suspend fun tryTransformMessage(ephemeral: Boolean, block: suspend (MessageCreateData) -> Unit) {
            val message = runDynamicHook(event, ephemeral) {
                val data = TransformData(event.target)
                messageTransformers.forEach { it.processMessage(data) }
                data.buildMessageOrNull()
            }

            if (message == null)
                return sendNoContentMessage()

            block(message)
        }

        val guild = event.guild
        // Experiment with the output being the application command interaction
        if (false && !guild.isDetached) {
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

                val fakedUser = event.target.inputUser
                webhookStore.getWebhook(channel)
                    .sendMessage(message)
                    .setUsername(fakedUser.effectiveName)
                    .setAvatarUrl(fakedUser.effectiveAvatarUrl)
                    .await()

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