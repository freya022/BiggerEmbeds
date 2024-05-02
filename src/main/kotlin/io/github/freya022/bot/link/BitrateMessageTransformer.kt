package io.github.freya022.bot.link

import io.github.freya022.bot.link.TransformData.AttachmentResult.KEEP
import io.github.freya022.bot.link.TransformData.AttachmentResult.REMOVE
import io.github.freya022.bot.utils.printOutputs
import io.github.freya022.bot.utils.redirectOutputs
import io.github.freya022.bot.utils.waitFor
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.FileUpload
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.fileSize

private val logger = KotlinLogging.logger { }
private const val maxBitrate: Long = 10 * 1024 * 1024
private const val targetBitrate: Long = (7.5 * 1024 * 1024).toLong()

@BService
data object BitrateMessageTransformer : MessageTransformer {
    override suspend fun processMessage(data: TransformData) {
        data.forEachRemainingAttachment { attachment ->
            if (!attachment.isVideo) return@forEachRemainingAttachment KEEP

            val url = attachment.proxyUrl
            if (getClipBitRate(url) <= maxBitrate) return@forEachRemainingAttachment KEEP

            val tmpPath = shrinkClip(url)
            withContext(Dispatchers.IO) {
                if (tmpPath.fileSize() > Message.MAX_FILE_SIZE) return@withContext
                data.builder.addFiles(FileUpload.fromData(tmpPath, attachment.fileName))
            }
            data.addCallback { withContext(Dispatchers.IO) { tmpPath.deleteIfExists() } }

            REMOVE
        }
    }

    private suspend fun getClipBitRate(url: String): Long = withContext(Dispatchers.IO) {
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()
        ProcessBuilder()
            .command(
                "ffprobe",
                "-v", "quiet",
                "-select_streams", "v:0",
                "-show_entries", "stream=bit_rate",
                "-of", "default=noprint_wrappers=1:nokey=1",
                url
            )
            .start()
            .redirectOutputs(outputStream, errorStream)
            .waitFor(logger, outputStream, errorStream)

        try {
            outputStream.toByteArray().decodeToString().trim().toLong()
        } catch (e: Exception) {
            logger.error { "Could not get bit rate from $url" }
            logger.error { "Decoded output: '${outputStream.toByteArray().decodeToString().trim()}'" }
            printOutputs(logger, outputStream, errorStream)
            throw e
        }
    }

    private suspend fun shrinkClip(url: String): Path = withContext(Dispatchers.IO) {
        val tempFile = createTempFile(suffix = ".mp4")
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()
        ProcessBuilder()
            .directory(tempFile.parent.toFile())
            .command(
                "ffmpeg", "-y", // Overwrite as the output file is already created
                "-i", url,
                "-c:v", "libx264",
                "-b:v", targetBitrate.toString(),
                "-movflags", "faststart",
                "-vf", "scale=1920:-1",
                "-c:a", "copy",
                tempFile.absolutePathString()
            )
            .start()
            .redirectOutputs(outputStream, errorStream)
            .waitFor(logger, outputStream, errorStream)

        tempFile
    }
}