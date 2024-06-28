package io.github.freya022.bot.link

import io.github.freya022.bot.link.TransformData.AttachmentResult.KEEP
import io.github.freya022.bot.link.TransformData.AttachmentResult.REMOVE
import io.github.freya022.bot.utils.Size
import io.github.freya022.bot.utils.Size.Companion.bits
import io.github.freya022.bot.utils.Size.Companion.bytes
import io.github.freya022.bot.utils.Size.Companion.kilobits
import io.github.freya022.bot.utils.Size.Companion.megabits
import io.github.freya022.bot.utils.Size.Companion.megabytes
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
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger { }
private val maxBitrate = 15.megabits
private val targetBitrate = 7500.kilobits

@BService
data object BitrateMessageTransformer : MessageTransformer {
    override suspend fun processMessage(data: TransformData) {
        data.forEachRemainingAttachment { attachment ->
            if (!attachment.isVideo) return@forEachRemainingAttachment KEEP
            if (attachment.size.bytes > 100.megabytes) return@forEachRemainingAttachment KEEP

            val url = attachment.proxyUrl
            val clipBitRate = getClipBitRate(url)
            if (clipBitRate <= maxBitrate) return@forEachRemainingAttachment KEEP

            // clip bit rate = clip size
            // shrunk bit rate = ?
            val plannedSize = targetBitrate * attachment.size.bytes / clipBitRate
            if (plannedSize > Message.MAX_FILE_SIZE.bytes) {
                logger.debug { "Not reducing clip due to exceeded planned size of $plannedSize : ${attachment.proxyUrl}" }
                return@forEachRemainingAttachment KEEP
            }

            val (tmpPath, duration) = measureTimedValue { shrinkClip(url) }
            withContext(Dispatchers.IO) {
                val fileSize = tmpPath.fileSize().bytes
                if (fileSize > Message.MAX_FILE_SIZE.bytes) {
                    logger.debug { "Not sending shrunk clip as it is larger than expected ($fileSize) : " }
                    return@withContext
                }
                logger.debug { "Shrunk file from ${attachment.size.bytes} to $fileSize in ${duration.toString(DurationUnit.SECONDS, decimals = 3)}" }
                data.builder.addFiles(FileUpload.fromData(tmpPath, attachment.fileName))
            }
            data.addCallback { withContext(Dispatchers.IO) { tmpPath.deleteIfExists() } }

            REMOVE
        }
    }

    private suspend fun getClipBitRate(url: String): Size = withContext(Dispatchers.IO) {
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
            outputStream.toByteArray().decodeToString().trim().toLong().bits
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
                "-b:v", targetBitrate.bits.toString(),
                "-movflags", "faststart",
                "-preset", "superfast",
                "-vf", "scale='min(iw,1920)':-1",
                "-c:a", "copy",
                tempFile.absolutePathString()
            )
            .start()
            .redirectOutputs(outputStream, errorStream)
            .waitFor(logger, outputStream, errorStream)

        tempFile
    }
}