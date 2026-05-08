package io.github.freya022.bot.video

import io.github.freya022.bot.utils.Size
import io.github.freya022.bot.utils.printOnErrorCode
import io.github.freya022.bot.utils.waitFor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createTempFile

private val logger = KotlinLogging.logger { }

// AV1 is better at low bitrate since x264 would produce a blurry mess
// but at higher bitrates and similar encoding speed (3-4x speed, preset 10 for AV1 and superfast for x264), x264 seems to be smoother looking, less blocky.
// So we'll keep using x264 since high bitrate clips are much more likely to be short (and thus keep a high bitrate without it being insane)

// ffmpeg -v error -i input.mp4 -c:v libsvtav1 -r 48 -preset 10 -minrate 250k -b:v 2000k -pass 1 -an -f null /dev/null
// ffmpeg -v error -i input.mp4 -c:v libsvtav1 -r 48 -preset 10 -minrate 250k -b:v 2000k -pass 2 -c:a libopus -b:a 96k output.webm

// ffmpeg -v error -i input.mp4 -c:v libx264 -r 48 -preset superfast -minrate 250k -b:v 6400k -pass 1 -an -f null /dev/null
// ffmpeg -v error -i input.mp4 -c:v libx264 -r 48 -preset superfast -minrate 250k -b:v 6400k -pass 2 -c:a libopus -b:a 96k -movflags +faststart output.mp4

object VideoShrinker {
    private val mutex = Mutex()

    suspend fun shrink(url: String, targetBitrate: Size): Path = mutex.withLock {
        doFirstPass(url, targetBitrate)
        doSecondPass(url, targetBitrate)
    }

    private suspend fun doFirstPass(url: String, targetBitrate: Size): Unit = withContext(Dispatchers.IO) {
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()
        ProcessBuilder()
            .directory(File(System.getProperty("java.io.tmpdir")))
            .command(
                "ffmpeg",
                "-i", url,
                "-c:v", "libx264",
                "-r", "48",
                "-preset", "superfast",
                "-vf", "scale='min(iw,1920)':-1",
                "-minrate", "250k",
                "-b:v", targetBitrate.bits.toString(),
                "-pass", "1",
                "-an",
                "-f", "null",
                "/dev/null",
            )
            .start()
            .waitFor(outputStream, errorStream)
            .printOnErrorCode(logger, outputStream, errorStream)
    }

    private suspend fun doSecondPass(url: String, targetBitrate: Size): Path = withContext(Dispatchers.IO) {
        val outputFile = createTempFile("output", ".mp4")

        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()
        ProcessBuilder()
            .directory(File(System.getProperty("java.io.tmpdir")))
            .redirectErrorStream(true)
            .command(
                "ffmpeg",
                "-i", url,
                "-c:v", "libx264",
                // 48 fps
                "-r", "48",
                "-preset", "superfast",
                // Scale down to 1080p
                "-vf", "scale='min(iw,1920)':-1",
                "-minrate", "250k",
                "-b:v", targetBitrate.bits.toString(),
                "-pass", "2",
                "-c:a", "libopus",
                "-b:a", "96k",
                "-movflags", "+faststart",
                "-y", // Temp file created above, overwrite
                outputFile.toString(),
            )
            .start()
            .waitFor(outputStream, errorStream)
            .printOnErrorCode(logger, outputStream, errorStream)

        outputFile
    }
}
