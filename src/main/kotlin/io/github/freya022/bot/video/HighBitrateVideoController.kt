package io.github.freya022.bot.video

import dev.freya02.botcommands.jda.ktx.components.MediaGalleryItem
import io.github.freya022.bot.utils.Size
import io.github.freya022.bot.utils.Size.Companion.bits
import io.github.freya022.bot.utils.Size.Companion.bytes
import io.github.freya022.bot.utils.Size.Companion.kilobits
import io.github.freya022.bot.utils.printOutputs
import io.github.freya022.bot.utils.waitFor
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.FileUpload
import java.io.ByteArrayOutputStream
import kotlin.io.path.fileSize
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger { }

// 10000 kbps
private val MAX_BITRATE = 10000.kilobits

@BService
class HighBitrateVideoController {
    data class ShrunkVideos(val newAttachments: List<FileUpload>, val items: List<MediaGalleryItem>)

    suspend fun tryShrinkVideos(message: Message): ShrunkVideos? {
        val attachments = message.attachments
        if (attachments.isEmpty()) return null

        // We want to send back the same attachments, while replacing those with a bitrate too high
        val newAttachments = arrayListOf<FileUpload>()
        val galleryItems: List<MediaGalleryItem> = attachments.mapIndexed { i, attachment ->
            val url = attachment.proxyUrl
            if (!attachment.isVideo) return@mapIndexed MediaGalleryItem(url)

            val (duration, bitrate) = getClipStats(url)
            if (bitrate <= MAX_BITRATE) return@mapIndexed MediaGalleryItem(url)

            // bytes => bytes/s => b/s (bits per second)
            val targetBitrate = (Message.MAX_FILE_SIZE / duration * 8).coerceAtMost(MAX_BITRATE.bits.toDouble())

            val (outputPath, shrinkTime) = measureTimedValue { VideoShrinker.shrink(url, targetBitrate.bits) }
            logger.debug { "Shrunk file #$i from ${message.idLong} from ${attachment.size.bytes} to ${outputPath.fileSize().bytes} in ${shrinkTime.toString(DurationUnit.SECONDS, decimals = 3)}" }

            val upload = FileUpload.fromData(outputPath)
            newAttachments.add(upload)
            MediaGalleryItem(upload)
        }

        if (newAttachments.isEmpty()) return null

        return ShrunkVideos(newAttachments, galleryItems)
    }

    private suspend fun getClipStats(url: String): ClipStats = withContext(Dispatchers.IO) {
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()
        ProcessBuilder()
            .command(
                "ffprobe",
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=duration,bit_rate",
                "-of", "default=noprint_wrappers=1:nokey=1",
                url
            )
            .start()
            .waitFor(logger, outputStream, errorStream)

        try {
            val output = outputStream.toByteArray().decodeToString().trim()
            val (duration, bitrate) = output.lines()
            ClipStats(duration.toDouble(), bitrate.toLong().bits)
        } catch (e: Exception) {
            logger.error { "Could not get clip stats from $url" }
            printOutputs(logger, outputStream, errorStream)
            throw e
        }
    }

    private data class ClipStats(val duration: Double, val bitrate: Size)
}
