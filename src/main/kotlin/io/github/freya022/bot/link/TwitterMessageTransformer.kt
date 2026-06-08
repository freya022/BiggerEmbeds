package io.github.freya022.bot.link

import dev.freya02.botcommands.jda.ktx.components.row
import io.github.freya022.botcommands.api.core.service.annotations.BService
import net.dv8tion.jda.api.components.buttons.Button
import okhttp3.HttpUrl.Companion.toHttpUrl

@BService
data object TwitterMessageTransformer : MessageTransformer {
    private const val TARGET_HOST = "fxtwitter.com"

    private val urlRegex = run {
        val replacedHosts = listOf(
            "twitter.com", "x.com",
            "nitter.net",
            "fxtwitter.com", "fixupx.com",
            "vxtwitter.com", "fixvx.com",
        )
        val quotedHosts = replacedHosts.joinToString("|") { Regex.escape(it) }
        Regex("""https://(?:${quotedHosts})\S*""")
    }

    override suspend fun processMessage(data: TransformData) {
        val urls = arrayListOf<String>()
        val replaced = urlRegex.replace(data.content) {
            it.value.toHttpUrl()
                .newBuilder()
                .host(TARGET_HOST)
                .query(null)
                .fragment(null)
                .toString()
                .also(urls::add)
        }

        if (urls.isEmpty()) return

        fun String.asTwitterUrl() = replaceFirst(TARGET_HOST, "twitter.com")
        fun String.asXUrl() = replaceFirst(TARGET_HOST, "x.com")
        fun String.asNitterUrl() = replaceFirst(TARGET_HOST, "nitter.net")

        data.setContent(replaced)
        if (urls.size == 1) {
            data.addComponents(
                row(
                    Button.link(urls.first().asTwitterUrl(), "Twitter"),
                    Button.link(urls.first().asXUrl(), "X"),
                    Button.link(urls.first().asNitterUrl(), "Nitter"),
                )
            )
        } else {
            val buttons = urls.flatMapIndexed { i, url ->
                listOf(
                    Button.link(url.asTwitterUrl(), "See #${i + 1} on Twitter"),
                    Button.link(url.asXUrl(), "See #${i + 1} on X"),
                    Button.link(url.asNitterUrl(), "See #${i + 1} on Nitter"),
                )
            }
            data.addComponents(buttons.chunked(4) { it.row() }.take(5))
        }
    }
}
