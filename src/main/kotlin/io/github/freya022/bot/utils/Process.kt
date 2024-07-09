package io.github.freya022.bot.utils

import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

suspend fun Process.waitFor(
    logger: KLogger,
    outputStream: ByteArrayOutputStream,
    errorStream: ByteArrayOutputStream
): Process = withContext(Dispatchers.IO) {
    // Redirect outputs and wait, do not return until both jobs are done
    coroutineScope {
        launch { redirectStream(outputStream, this@waitFor.inputStream) }
        launch { redirectStream(errorStream, this@waitFor.errorStream) }

        val exitCode = waitFor()
        if (exitCode != 0) {
            printOutputs(logger, outputStream, errorStream)

            logger.error { "Process exited with code: $exitCode" }
        }
    }

    this@waitFor
}

fun printOutputs(
    logger: KLogger,
    outputStream: ByteArrayOutputStream,
    errorStream: ByteArrayOutputStream
) {
    val outputString = outputStream.toByteArray().decodeToString()
    when {
        outputString.isNotBlank() -> logger.warn { "Output:\n$outputString" }
        else -> logger.warn { "No output" }
    }

    val errorString = errorStream.toByteArray().decodeToString()
    when {
        errorString.isNotBlank() -> logger.error { "Error output:\n$errorString" }
        else -> logger.warn { "No error output" }
    }
}

suspend fun redirectStream(outputStream: OutputStream, processStream: InputStream) = withContext(Dispatchers.IO) {
    outputStream.bufferedWriter().use { writer ->
        processStream.bufferedReader().use { reader ->
            var readLine: String?
            while (reader.readLine().also { readLine = it } != null) {
                writer.append(readLine + System.lineSeparator())
            }
        }
    }
}