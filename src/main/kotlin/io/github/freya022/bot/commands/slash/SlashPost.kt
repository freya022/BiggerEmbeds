package io.github.freya022.bot.commands.slash

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.bot.WebhookStore
import io.github.freya022.bot.link.LinkTransformer
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption
import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.api.core.service.getInterfacedServices
import io.github.freya022.botcommands.api.core.utils.namedDefaultScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import kotlin.time.Duration.Companion.milliseconds

@Command
class SlashPost(
    context: BContext,
    private val webhookStore: WebhookStore
) : ApplicationCommand() {
    private val dynamicHookScope = namedDefaultScope("/post dynamic hook", 1)
    private val linkTransformers = context.getInterfacedServices<LinkTransformer>()

    @JDASlashCommand(name = "post")
    suspend fun onSlashPost(event: GuildSlashEvent, @SlashOption(description = "The post content") post: String) {
        val channel = event.channel
        if (channel !is IWebhookContainer)
            return event.reply_("This channel cannot have webhooks", ephemeral = true).queue()

        val job = dynamicHookScope.launch {
            delay(500.milliseconds)
            event.deferReply(true).await()
        }

        try {
            var builder = MessageCreateBuilder().setContent(post)
            for (linksWatcher in linkTransformers) {
                val newData = linksWatcher.editMessageIfNeededOrNull(builder)
                if (newData != null) builder = newData
            }

            webhookStore.getWebhook(channel)
                .sendMessage(builder.build())
                .setUsername(event.member.effectiveName)
                .setAvatarUrl(event.member.effectiveAvatarUrl)
                .await()
        } finally {
            job.cancelAndJoin()
        }

        if (event.isAcknowledged) {
            event.hook.deleteOriginal().queue()
        } else {
            event.reply_("OK", ephemeral = true)
                .flatMap(InteractionHook::deleteOriginal)
                .queue()
        }
    }
}