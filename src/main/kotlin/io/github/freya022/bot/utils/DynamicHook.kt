package io.github.freya022.bot.utils

import dev.minn.jda.ktx.coroutines.await
import io.github.freya022.botcommands.api.core.utils.namedDefaultScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Duration.Companion.seconds

private val dynamicHookScope = namedDefaultScope("Dynamic hook", 1)

@OptIn(ExperimentalContracts::class)
suspend fun <R> runDynamicHook(event: IReplyCallback, ephemeral: Boolean, block: suspend () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val job = dynamicHookScope.launch {
        delay(1.seconds)
        event.deferReply(ephemeral).await()
    }

    try {
        return block()
    } finally {
        job.cancelAndJoin()
    }
}