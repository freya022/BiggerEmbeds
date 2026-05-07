package io.github.freya022.bot.config

import com.google.gson.Gson
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText

data class Config(
    val token: String,
    val ownerIds: List<Long>,
    val testGuildIds: List<Long>,
    val videoListenerGuildIds: List<Long>,
) {

    companion object {

        @get:BService
        val instance: Config by lazy {
            val logger = KotlinLogging.logger { }
            val configFilePath: Path = Environment.configFolder.resolve("config.json")
            logger.info { "Loading configuration at ${configFilePath.absolutePathString()}" }

            Gson().fromJson(configFilePath.readText(), Config::class.java)
        }
    }
}
