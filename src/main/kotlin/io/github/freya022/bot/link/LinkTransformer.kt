package io.github.freya022.bot.link

import io.github.freya022.botcommands.api.core.service.annotations.InterfacedService

@InterfacedService(acceptMultiple = true)
sealed interface LinkTransformer {
    suspend fun processMessage(data: TransformData)
}