package io.github.freya022.bot.link

import io.github.freya022.bot.WebhookStore
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.namedDefaultScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.ByteArrayOutputStream

private val logger = KotlinLogging.logger { }

@BService
class TokTokLinksWatcher(webhookStore: WebhookStore) : AbstractLinksWatcher(webhookStore) {
    private val outputScope = namedDefaultScope("TokTok yt-dlp output", 2)

    override fun editMessageIfNeededOrNull(editableMessage: EditableMessage): EditableMessage? {
        val files = hashMapOf<String, ByteArray>()
        val replaced = urlRegex.replace(editableMessage.content) { matchResult ->
            val url = matchResult.value
            val httpUrl = url.toHttpUrlOrNull() ?: return@replace url
            if (httpUrl.host != "vm.tiktok.com" && httpUrl.host != "www.tiktok.com") return@replace url

            val byteStream = ByteArrayOutputStream(1024 * 1024 * 8)
            val errorStream = ByteArrayOutputStream(1024 * 8)
            val process = ProcessBuilder(
                "yt-dlp",
                "-f", "bv*[filesize<=20M][vcodec=h264]+ba/b[filesize<=20M][vcodec=h264] / wv*[filesize<=25M][vcodec=h264]+ba/w[filesize<=25M][vcodec=h264]",
                "-o", "-",
                "-q", "--verbose",
                url
            ).start()

            outputScope.launch { process.inputStream.buffered(1024 * 1024).transferTo(byteStream) }
            outputScope.launch { process.errorStream.buffered().transferTo(errorStream) }

            if (process.waitFor() != 0) {
                logger.error("yt-dlp exited with ${process.exitValue()}:\n${errorStream.toByteArray().decodeToString()}")
            }

            files["${httpUrl.pathSegments.last { it.isNotBlank() }}.mp4"] = byteStream.toByteArray()
            ""
        }

        return when {
            files.isNotEmpty() -> editableMessage.also {
                it.content = replaced
                it.files += files
            }
            else -> null
        }
    }
}