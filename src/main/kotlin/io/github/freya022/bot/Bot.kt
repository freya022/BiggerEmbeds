package io.github.freya022.bot

import io.github.freya022.bot.config.Config
import io.github.freya022.botcommands.api.core.JDAService
import io.github.freya022.botcommands.api.core.events.BReadyEvent
import io.github.freya022.botcommands.api.core.light
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.enumSetOf
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.hooks.IEventManager
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag

@BService
class Bot(private val config: Config) : JDAService() {

    override val intents: Set<GatewayIntent> = enumSetOf(
        GatewayIntent.DIRECT_MESSAGES /* JDA falsely says both intents are required */,
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.MESSAGE_CONTENT
    )

    override val cacheFlags: Set<CacheFlag> = emptySet()

    override fun createJDA(event: BReadyEvent, eventManager: IEventManager) {
        light(config.token, activity = Activity.customStatus("I need more bullets")) {

        }
    }
}