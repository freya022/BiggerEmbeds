package io.github.freya022.bot

import io.github.freya022.botcommands.api.core.annotations.BEventListener
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.session.ReadyEvent

private val logger = KotlinLogging.logger { }

@BService
class ReadyListener {
    @BEventListener
    fun onReady(event: ReadyEvent) {
        val jda = event.jda

        //Print some information about the bot
        logger.info { "Bot connected as ${jda.selfUser.name}" }
        logger.info { "The bot is present on these guilds :" }
        for (guild in jda.guildCache) {
            logger.info { "\t- ${guild.name} (${guild.id})" }
        }
    }
}