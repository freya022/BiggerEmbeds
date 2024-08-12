package io.github.freya022.bot.config

import com.google.gson.Gson
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText


data class Config(val token: String,
                  val ownerIds: List<Long>,
                  val prefixes: List<String>,
                  val testGuildIds: List<Long>,
) {
    companion object {
        private val logger = KotlinLogging.logger { }

        private val configFilePath: Path = Environment.configFolder.resolve("config.json")

        @get:BService
        val instance: Config by lazy {
            logger.info { "Loading configuration at ${configFilePath.absolutePathString()}" }

            return@lazy Gson().fromJson(configFilePath.readText(), Config::class.java)
        }
    }
}
