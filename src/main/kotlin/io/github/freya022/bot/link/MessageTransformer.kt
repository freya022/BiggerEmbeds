package io.github.freya022.bot.link

import io.github.freya022.botcommands.api.core.service.annotations.InterfacedService
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

@InterfacedService(acceptMultiple = true)
sealed interface MessageTransformer {
    suspend fun processMessage(data: TransformData)
}

context(_: MessageTransformer)
fun suppressedLinkAwareBuilder(link: String, block: HttpUrl.Builder.(original: HttpUrl) -> Unit): String {
    val isSuppressed = link.startsWith('<') && link.endsWith('>')
    val url = if (isSuppressed) link.substring(1, link.length - 1) else link

    val newUrl = url.toHttpUrl().let { original ->
        original
            .newBuilder()
            .apply { block(original) }
            .toString()
    }

    return when {
        isSuppressed -> "<$newUrl>"
        else -> newUrl
    }
}
