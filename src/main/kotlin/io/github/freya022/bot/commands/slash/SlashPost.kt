package io.github.freya022.bot.commands.slash

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.bot.WebhookStore
import io.github.freya022.bot.link.MessageTransformer
import io.github.freya022.bot.link.TransformData
import io.github.freya022.bot.utils.use
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData
import io.github.freya022.botcommands.api.core.utils.namedDefaultScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.interactions.IntegrationType.GUILD_INSTALL
import net.dv8tion.jda.api.interactions.IntegrationType.USER_INSTALL
import net.dv8tion.jda.api.interactions.InteractionContextType.GUILD
import net.dv8tion.jda.api.interactions.InteractionContextType.PRIVATE_CHANNEL
import net.dv8tion.jda.api.interactions.InteractionHook
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Duration.Companion.milliseconds

@Command
class SlashPost(
    private val webhookStore: WebhookStore,
    private val messageTransformers: List<MessageTransformer>
) : ApplicationCommand() {
    private val dynamicHookScope = namedDefaultScope("/post dynamic hook", 1)

    @JDASlashCommand(name = "post", description = "Send media with better embeds")
    @TopLevelSlashCommandData(
        contexts = [GUILD, PRIVATE_CHANNEL],
        integrationTypes = [GUILD_INSTALL, USER_INSTALL],
    )
    suspend fun onSlashPost(event: GlobalSlashEvent, @SlashOption(description = "The post content") post: String) {
        val guild = event.guild
        if (guild?.isDetached == false) {
            val channel = event.channel
            if (channel !is IWebhookContainer)
                return event.reply_("Can only run in channels with webhooks", ephemeral = true).queue()
            if (!guild.selfMember.hasPermission(channel, Permission.MANAGE_WEBHOOKS))
                return event.reply_("I require the permission to manage webhooks", ephemeral = true).queue()

            runDynamicHook(event, ephemeral = true) {
                TransformData(post).use { data ->
                    messageTransformers.forEach { it.processMessage(data) }
                    val message = data.buildMessage()

                    webhookStore.getWebhook(channel)
                        .sendMessage(message)
                        .setUsername(event.member?.effectiveName ?: event.user.effectiveName)
                        .setAvatarUrl(event.member?.effectiveAvatarUrl ?: event.user.effectiveName)
                        .await()
                }
            }

            if (event.isAcknowledged) {
                event.hook.deleteOriginal().queue()
            } else {
                event.reply_("OK", ephemeral = true)
                    .flatMap(InteractionHook::deleteOriginal)
                    .queue()
            }
        } else {
            // Friends, GDMs and detached guilds
            TransformData(post).use { data ->
                val message = runDynamicHook(event, ephemeral = false) {
                    messageTransformers.forEach { it.processMessage(data) }
                    data.buildMessage()
                }

                if (event.isAcknowledged) {
                    event.hook.sendMessage(message).queue()
                } else {
                    event.reply(message).queue()
                }
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    private suspend inline fun <R> runDynamicHook(event: GlobalSlashEvent, ephemeral: Boolean, block: () -> R): R {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        val job = dynamicHookScope.launch {
            delay(500.milliseconds)
            event.deferReply(ephemeral).await()
        }

        try {
            return block()
        } finally {
            job.cancelAndJoin()
        }
    }
}