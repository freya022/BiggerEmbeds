package io.github.freya022.bot

import dev.minn.jda.ktx.coroutines.await
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.awaitCatching
import io.github.freya022.botcommands.api.core.utils.handle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Icon
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.RestAction
import kotlinx.coroutines.future.await as futureAwait

@BService
class WebhookStore {

    private val lock = Mutex()
    private val webhooks: MutableMap<Long, Webhook> = hashMapOf()

    suspend fun sendMessage(
        channel: IWebhookContainer,
        sendFunction: suspend (Webhook) -> RestAction<Message>
    ): Message = lock.withLock {
        return sendFunction(getWebhook(channel))
            .awaitCatching()
            // In case the (cached) webhook was deleted in between, invalidate and retry *once*
            .handle(ErrorResponse.UNKNOWN_WEBHOOK) {
                webhooks.remove(channel.idLong)
                sendFunction(getWebhook(channel)).await()
            }
            .getOrThrow()
    }

    private suspend fun getWebhook(channel: IWebhookContainer): Webhook {
        return webhooks.getOrPut(channel.idLong) {
            channel.retrieveWebhooks().await().find { it.ownerAsUser?.idLong == channel.jda.selfUser.idLong }
                ?: createWebhook(channel)
        }
    }

    private suspend fun createWebhook(channel: IWebhookContainer): Webhook {
        val avatarIcon = channel.jda.selfUser.effectiveAvatar.let { effectiveAvatar ->
            val bytes = withContext(Dispatchers.IO) { effectiveAvatar.download().futureAwait().readAllBytes() }
            val iconType = Icon.IconType.fromExtension(effectiveAvatar.url.substringAfterLast('.', missingDelimiterValue = "jpeg"))
            Icon.from(bytes, iconType)
        }
        return channel.createWebhook(channel.jda.selfUser.effectiveName)
            .setAvatar(avatarIcon)
            .await()
    }
}