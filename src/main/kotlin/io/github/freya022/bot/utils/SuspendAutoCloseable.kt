package io.github.freya022.bot.utils

interface SuspendAutoCloseable {
    suspend fun close()
}

suspend inline fun <T : SuspendAutoCloseable, R> T.use(block: (T) -> R): R {
    return try {
        block(this)
    } finally {
        close()
    }
}