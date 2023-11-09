package io.github.freya022.bot.link

import io.github.freya022.botcommands.api.core.service.annotations.InterfacedService
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

@InterfacedService(acceptMultiple = true)
sealed interface LinkTransformer {
    fun editMessageIfNeededOrNull(builder: MessageCreateBuilder): MessageCreateBuilder?
}