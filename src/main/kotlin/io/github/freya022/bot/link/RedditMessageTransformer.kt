package io.github.freya022.bot.link

import dev.freya02.botcommands.jda.ktx.components.row
import io.github.freya022.botcommands.api.core.service.annotations.BService
import net.dv8tion.jda.api.components.buttons.Button

@BService
data object RedditMessageTransformer : MessageTransformer {
    private const val TARGET_HOST = "vxreddit.com"

    private val urlRegex = run {
        val replacedHosts = listOf(
            "reddit.com",
            "reddit.com",
            "rxddit.com",
            "vxreddit.com",
        )
        val quotedHosts = replacedHosts.joinToString("|") { Regex.escape(it) }
        Regex("""<?https://(?:www\.)?(?:${quotedHosts})/r/\S*>?""")
    }

    override suspend fun processMessage(data: TransformData) {
        val urls = arrayListOf<String>()
        val replaced = urlRegex.replace(data.content) { matchResult ->
            suppressedLinkAwareBuilder(matchResult.value) { original ->
                val subdomainParts = original.host.split('.').dropLast(2)
                // Support for old reddit for example
                host((subdomainParts + TARGET_HOST).joinToString("."))
                query(null)
                fragment(null)
            }.also(urls::add)
        }

        if (urls.isEmpty()) return

        fun String.asRedditUrl() = replaceFirst(TARGET_HOST, "reddit.com")

        data.setContent(replaced)
        if (urls.size == 1) {
            data.addComponents(
                row(Button.link(urls.first().asRedditUrl(), "See on Reddit")),
            )
        } else {
            val buttons = urls.flatMapIndexed { i, url ->
                listOf(
                    Button.link(url.asRedditUrl(), "See #${i + 1} on Reddit"),
                )
            }
            data.addComponents(buttons.chunked(4) { it.row() }.take(5))
        }
    }
}
