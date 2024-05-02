package io.github.freya022.bot.utils

interface SuspendAutoCloseable {
    suspend fun close()
}

suspend inline fun <T : SuspendAutoCloseable> T.use(block: (T) -> Unit) {
    try {
        block(this)
    } finally {
        close()
    }
}