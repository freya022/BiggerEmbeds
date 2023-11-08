package io.github.freya022.bot

import dev.minn.jda.ktx.coroutines.await
import io.github.freya022.botcommands.api.core.service.annotations.BService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Icon
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer

@BService
class WebhookStore {
    private val lock = Mutex()
    private val webhooks: MutableMap<Long, Webhook> = hashMapOf()

    suspend fun getWebhook(channel: IWebhookContainer) = lock.withLock {
        webhooks.getOrPut(channel.idLong) {
            channel.retrieveWebhooks().await().find { it.name == getWebhookName(channel) }
                ?: createWebhook(channel)
        }
    }

    private suspend fun createWebhook(channel: IWebhookContainer): Webhook {
        val avatarIcon = channel.jda.selfUser.effectiveAvatar.let { effectiveAvatar ->
            val bytes = withContext(Dispatchers.IO) { effectiveAvatar.download().await().readAllBytes() }
            val iconType = Icon.IconType.fromExtension(effectiveAvatar.url.substringAfterLast('.', missingDelimiterValue = "jpeg"))
            Icon.from(bytes, iconType)
        }
        return channel.createWebhook(getWebhookName(channel))
            .setAvatar(avatarIcon)
            .await()
    }

    private fun getWebhookName(channel: Channel) =
        channel.jda.selfUser.effectiveName
}