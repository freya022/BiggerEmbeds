package io.github.freya022.bot.link

import io.github.freya022.bot.utils.printOutputs
import io.github.freya022.bot.utils.waitFor
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.utils.FileUpload
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteExisting

private val logger = KotlinLogging.logger { }

@BService
class TokTokMessageTransformer : MessageTransformer {

    override suspend fun processMessage(data: TransformData) {
        var hasChanged = false
        urlRegex.findAll(data.content).forEach { matchResult ->
            val url = matchResult.value
            val httpUrl = url.toHttpUrlOrNull() ?: return@forEach
            if (httpUrl.host != "vm.tiktok.com" && httpUrl.host != "www.tiktok.com") return@forEach

            val outputFile = Files.createTempFile(httpUrl.pathSegments.last { it.isNotBlank() }, ".mp4")

            val outputStream = ByteArrayOutputStream()
            val errorStream = ByteArrayOutputStream()
            val process = ProcessBuilder()
                .command(
                    "yt-dlp",
                    // Best video+audio or video-only below 10 MB in h264
                    "-f", "b*[filesize<=10M][vcodec=h264]",
                    // Send output to temp file
                    "-o", outputFile.absolutePathString(),
                    // Overwrite temp file
                    "--force-overwrite",
                    // Quiet and no warnings
                    "-q", "--no-warnings",
                    url
                )
                .start()
                .waitFor(outputStream, errorStream)

            if (process.exitValue() != 0) {
                if ("Requested format is not available" in errorStream.toString()) {
                    outputFile.deleteExisting()
                    return@forEach
                }

                logger.error { "Could not download toktok @ $url" }
                printOutputs(logger, outputStream, errorStream)
                return@forEach
            }

            data.addFiles(FileUpload.fromData(outputFile))
            data.addCallback { outputFile.deleteExisting() }
            hasChanged = true
        }

        if (!hasChanged) return

        data.setContent(urlRegex.replace(data.content, ""))
    }
}
