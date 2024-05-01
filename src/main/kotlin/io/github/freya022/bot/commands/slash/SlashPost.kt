package io.github.freya022.bot.commands.slash

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.bot.WebhookStore
import io.github.freya022.bot.link.LinkTransformer
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
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.interactions.IntegrationType.GUILD_INSTALL
import net.dv8tion.jda.api.interactions.IntegrationType.USER_INSTALL
import net.dv8tion.jda.api.interactions.InteractionContextType.GUILD
import net.dv8tion.jda.api.interactions.InteractionContextType.PRIVATE_CHANNEL
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import kotlin.time.Duration.Companion.milliseconds

@Command
class SlashPost(
    private val webhookStore: WebhookStore,
    private val linkTransformers: List<LinkTransformer>
) : ApplicationCommand() {
    private val dynamicHookScope = namedDefaultScope("/post dynamic hook", 1)

    @JDASlashCommand(name = "post", description = "Send media with better embeds")
    @TopLevelSlashCommandData(
        contexts = [GUILD, PRIVATE_CHANNEL],
        integrationTypes = [GUILD_INSTALL, USER_INSTALL],
    )
    suspend fun onSlashPost(event: GlobalSlashEvent, @SlashOption(description = "The post content") post: String) {
        if (event.guild?.isDetached == false) {
            val channel = event.channel
            if (channel !is IWebhookContainer)
                return event.reply_("Can only run in channels with webhooks", ephemeral = true).queue()

            runDynamicHook(event, ephemeral = true) {
                webhookStore.getWebhook(channel)
                    .sendMessage(getMessageData(post))
                    .setUsername(event.member?.effectiveName ?: event.user.effectiveName)
                    .setAvatarUrl(event.member?.effectiveAvatarUrl ?: event.user.effectiveName)
                    .await()
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
            val messageData = runDynamicHook(event, ephemeral = false) {
                getMessageData(post)
            }

            if (event.isAcknowledged) {
                event.hook.sendMessage(messageData).queue()
            } else {
                event.reply(messageData).queue()
            }
        }
    }

    private suspend inline fun <R> runDynamicHook(event: GlobalSlashEvent, ephemeral: Boolean, block: () -> R): R {
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

    private fun getMessageData(post: String): MessageCreateData {
        var builder = MessageCreateBuilder().setContent(post)
        for (linksWatcher in linkTransformers) {
            val newData = linksWatcher.editMessageIfNeededOrNull(builder)
            if (newData != null) builder = newData
        }

        val messageCreateData = builder.build()
        return messageCreateData
    }
}