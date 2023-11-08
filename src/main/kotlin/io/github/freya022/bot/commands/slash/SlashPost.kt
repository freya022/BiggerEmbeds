package io.github.freya022.bot.commands.slash

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.bot.WebhookStore
import io.github.freya022.bot.link.AbstractLinksWatcher
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
import kotlin.time.Duration.Companion.milliseconds

@Command
class SlashPost(
    context: BContext,
    private val webhookStore: WebhookStore
) : ApplicationCommand() {
    private val dynamicHookScope = namedDefaultScope("/post dynamic hook", 1)
    private val linksWatchers = context.getInterfacedServices<AbstractLinksWatcher>()

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
            var editableMessage = AbstractLinksWatcher.EditableMessage.fromContent(post)
            for (linksWatcher in linksWatchers) {
                val newData = linksWatcher.editMessageIfNeededOrNull(editableMessage)
                if (newData != null) editableMessage = newData
            }

            val message = AbstractLinksWatcher.createMessage(event.member, editableMessage)
            webhookStore.getWebhook(channel).send(message).await()
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