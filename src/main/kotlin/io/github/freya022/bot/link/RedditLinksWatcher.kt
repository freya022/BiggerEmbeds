package io.github.freya022.bot.link

import io.github.freya022.bot.WebhookStore
import io.github.freya022.botcommands.api.core.service.annotations.BService
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@BService
class RedditLinksWatcher(webhookStore: WebhookStore): AbstractLinksWatcher(webhookStore) {
    override fun editMessageIfNeededOrNull(editableMessage: EditableMessage): EditableMessage? {
        val replaced = urlRegex.replace(editableMessage.content) {
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
            replaced != editableMessage.content -> editableMessage.also { it.content = replaced }
            else -> null
        }
    }
}