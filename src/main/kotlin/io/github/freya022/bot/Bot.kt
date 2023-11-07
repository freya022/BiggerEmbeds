package io.github.freya022.bot

import dev.minn.jda.ktx.jdabuilder.light
import io.github.freya022.bot.config.Config
import io.github.freya022.botcommands.api.core.JDAService
import io.github.freya022.botcommands.api.core.events.BReadyEvent
import io.github.freya022.botcommands.api.core.service.annotations.BService
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.hooks.IEventManager
import net.dv8tion.jda.api.requests.GatewayIntent

@BService
class Bot(private val config: Config) : JDAService() {
    override val intents: Set<GatewayIntent> = defaultIntents

    override fun createJDA(event: BReadyEvent, eventManager: IEventManager) {
        // You MUST disable enableCoroutines and set the event manager to the injected one
        light(config.token, intents = intents, enableCoroutines = false) {
            setActivity(Activity.customStatus("I need more bullets"))
            setEventManager(eventManager)
        }
    }
}