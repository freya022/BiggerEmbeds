package io.github.freya022.bot.link

import io.github.freya022.botcommands.api.core.service.annotations.BService
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@BService
data object RedditMessageTransformer : MessageTransformer {
    override suspend fun processMessage(data: TransformData) {
        val builder = data.builder
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

        if (replaced != builder.content)
            builder.setContent(replaced)
    }
}