package io.github.freya022.bot.link

import dev.minn.jda.ktx.interactions.components.row
import io.github.freya022.botcommands.api.core.service.annotations.BService
import net.dv8tion.jda.api.interactions.components.buttons.Button
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@BService
data object RedditMessageTransformer : MessageTransformer {
    override suspend fun processMessage(data: TransformData) {
        val builder = data.builder
        val urls = arrayListOf<String>()
        val replaced = urlRegex.replace(builder.content) {
            val httpUrl = it.value.toHttpUrlOrNull() ?: return@replace it.value
            if (!httpUrl.host.endsWith("reddit.com")) return@replace it.value

            httpUrl
                .newBuilder()
                .host(httpUrl.host.replaceFirst("reddit.com", "rxddit.com"))
                .query(null)
                .fragment(null)
                .toString()
                .also(urls::add)
        }

        if (urls.isEmpty()) return

        fun String.asRedditUrl() = replaceFirst("rxddit.com", "reddit.com")

        builder.setContent(replaced)
        if (urls.size == 1) {
            builder.addActionRow(
                Button.link(urls.first().asRedditUrl(), "See on Reddit"),
            )
        } else {
            val buttons = urls.flatMapIndexed { i, url ->
                listOf(
                    Button.link(url.asRedditUrl(), "See #${i + 1} on Reddit"),
                )
            }
            builder.addComponents(buttons.chunked(4) { it.row() }.take(5))
        }
    }
}