package io.github.freya022.bot.commands.text

import dev.freya02.botcommands.jda.ktx.durations.timeout
import dev.freya02.jda.emojis.unicode.UnicodeEmojis
import io.github.freya022.botcommands.api.core.BotOwners
import io.github.freya022.botcommands.api.core.annotations.BEventListener
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger { }

@BService
class TextExit {
    @BEventListener(ignoredIntents = [GatewayIntent.DIRECT_MESSAGES])
    fun onMessage(event: MessageReceivedEvent, owners: BotOwners) {
        val message = event.message
        val selfUser = event.jda.selfUser

        // Only allow owners
        if (event.author.idLong !in owners.ownerIds) {
            return
        }
        // Check prefixed with bot mention
        if (!message.mentions.isMentioned(selfUser)) {
            return
        }
        if (!message.contentRaw.startsWith(selfUser.asMention)) {
            return
        }
        // Check command
        val command = message.contentRaw.substringAfter(selfUser.asMention).trimStart()
        val commandName = command.substringBefore(' ')
        val reason = command.substringAfter(' ', missingDelimiterValue = "")

        if (commandName != "exit") {
            return
        }

        logger.warn { "Exiting for reason: $reason" }

        message.addReaction(UnicodeEmojis.WHITE_CHECK_MARK)
            .timeout(5.seconds)
            .mapToResult()
            .queue { exitProcess(0) }
    }
}
