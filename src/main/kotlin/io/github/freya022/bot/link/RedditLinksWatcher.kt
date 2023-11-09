package io.github.freya022.bot.link

import io.github.freya022.bot.WebhookStore
import io.github.freya022.botcommands.api.core.service.annotations.BService
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@BService
class RedditLinksWatcher(webhookStore: WebhookStore): AbstractLinksWatcher(webhookStore) {
    override fun editMessageIfNeededOrNull(builder: MessageCreateBuilder): MessageCreateBuilder? {
        val replaced = urlRegex.replace(builder.content) {
            val httpUrl = it.value.toHttpUrlOrNull() ?: return@replace it.value
            if (!httpUrl.host.endsWith("reddit.com")) return@replace it.value

            httpUrl
                .newBuilder()
                .host(httpUrl.host.replaceFirst("reddit.com", "rxddit.com"))
                .query(null)
                .fragment(null)
                .toString()
        }

        return when {
            replaced != builder.content -> builder.also { it.setContent(replaced) }
            else -> null
        }
    }
}