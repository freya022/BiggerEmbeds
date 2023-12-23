package io.github.freya022.bot.link

import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.into
import io.github.freya022.botcommands.api.core.service.annotations.BService
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@BService
data object TwitterLinkTransformer : LinkTransformer {
    private val replacedHosts = setOf("twitter.com", "x.com", "nitter.net", "vxtwitter.com")

    override fun editMessageIfNeededOrNull(builder: MessageCreateBuilder): MessageCreateBuilder? {
        val nitterUrls = arrayListOf<String>()
        val replaced = urlRegex.replace(builder.content) {
            val httpUrl = it.value.toHttpUrlOrNull() ?: return@replace it.value
            if (httpUrl.host !in replacedHosts) return@replace it.value

            httpUrl
                .newBuilder()
                .host("vxtwitter.com")
                .query(null)
                .fragment(null)
                .toString()
                .also { url -> nitterUrls.add(url.replaceFirst("vxtwitter.com", "nitter.net")) }
        }

        return when {
            nitterUrls.isNotEmpty() -> builder.also { createBuilder ->
                createBuilder.setContent(replaced)
                if (nitterUrls.size == 1) {
                    createBuilder.addComponents(Button.link(nitterUrls.first(), "See on Nitter").into())
                } else {
                    val buttons = nitterUrls.mapIndexed { i, url -> Button.link(url, "See #${i + 1} on Nitter") }
                    createBuilder.addComponents(buttons.chunked(5) { it.row() })
                }
            }
            else -> null
        }
    }
}