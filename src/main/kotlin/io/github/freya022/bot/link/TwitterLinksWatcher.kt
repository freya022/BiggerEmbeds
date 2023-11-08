package io.github.freya022.bot.link

import io.github.freya022.bot.WebhookStore
import io.github.freya022.botcommands.api.core.service.annotations.BService
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@BService
class TwitterLinksWatcher(webhookStore: WebhookStore) : AbstractLinksWatcher(webhookStore) {
    override fun editMessageIfNeededOrNull(editableMessage: EditableMessage): EditableMessage? {
        val replaced = urlRegex.replace(editableMessage.content) {
            val httpUrl = it.value.toHttpUrlOrNull() ?: return@replace it.value
            if (httpUrl.host != "twitter.com" && httpUrl.host != "x.com") return@replace it.value

            httpUrl
                .newBuilder()
                .host("vxtwitter.com")
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