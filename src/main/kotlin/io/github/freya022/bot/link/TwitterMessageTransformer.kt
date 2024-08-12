package io.github.freya022.bot.link

import dev.minn.jda.ktx.interactions.components.row
import io.github.freya022.botcommands.api.core.service.annotations.BService
import net.dv8tion.jda.api.interactions.components.buttons.Button
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@BService
data object TwitterMessageTransformer : MessageTransformer {
    private const val targetHost = "fxtwitter.com"
    private val replacedHosts = setOf("twitter.com", "x.com", "nitter.net", "vxtwitter.com", "fxtwitter.com", "fixupx.com")

    override suspend fun processMessage(data: TransformData) {
        val builder = data.builder
        val urls = arrayListOf<String>()
        val replaced = urlRegex.replace(builder.content) {
            val httpUrl = it.value.toHttpUrlOrNull() ?: return@replace it.value
            if (httpUrl.host !in replacedHosts) return@replace it.value

            httpUrl
                .newBuilder()
                .host(targetHost)
                .query(null)
                .fragment(null)
                .toString()
                .also(urls::add)
        }

        if (urls.isEmpty()) return

        fun String.asTwitterUrl() = replaceFirst(targetHost, "twitter.com")
        fun String.asXUrl() = replaceFirst(targetHost, "x.com")

        builder.setContent(replaced)
        if (urls.size == 1) {
            builder.addActionRow(
                Button.link(urls.first().asTwitterUrl(), "See on Twitter"),
                Button.link(urls.first().asXUrl(), "See on X"),
            )
        } else {
            val buttons = urls.flatMapIndexed { i, url ->
                listOf(
                    Button.link(url.asTwitterUrl(), "See #${i + 1} on Twitter"),
                    Button.link(url.asXUrl(), "See #${i + 1} on X"),
                )
            }
            builder.addComponents(buttons.chunked(4) { it.row() }.take(5))
        }
    }
}