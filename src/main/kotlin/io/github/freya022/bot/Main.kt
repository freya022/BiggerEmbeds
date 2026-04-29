package io.github.freya022.bot

import ch.qos.logback.classic.ClassicConstants as LogbackConstants
import io.github.freya022.bot.config.Config
import io.github.freya022.bot.config.Environment
import io.github.freya022.botcommands.api.core.BotCommands
import io.github.freya022.botcommands.api.core.config.DevConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess

private val logger by lazy { KotlinLogging.logger {} } // Must not load before system property is set

private const val mainPackageName = "io.github.freya022.bot"

object Main {
    @JvmStatic
    fun main(args: Array<out String>) {
        try {
            System.setProperty(LogbackConstants.CONFIG_FILE_PROPERTY, Environment.logbackConfigPath.absolutePathString())
            logger.info { "Loading logback configuration at ${Environment.logbackConfigPath.absolutePathString()}" }

            val config = Config.instance

            BotCommands.create {
                disableExceptionsInDMs = Environment.isDev

                addPredefinedOwners(*config.ownerIds.toLongArray())

                addSearchPath(mainPackageName)

                applicationCommands {
                    @OptIn(DevConfig::class)
                    disableAutocompleteCache = Environment.isDev

                    // Check command updates based on Discord's commands.
                    // This is only useful during development,
                    // as you can develop on multiple machines (but not simultaneously!).
                    // Using this in production is only going to waste API requests.
                    fileCache {
                        @OptIn(DevConfig::class)
                        checkOnline = Environment.isDev
                    }

                    // Guilds in which `@Test` commands will be inserted
                    testGuildIds += config.testGuildIds
                }

                coroutineScopes {
                    eventManagerScopeFactory = defaultFactory("BiggerEmbeds Coroutine", 4)
                }
            }

            // There is no JDABuilder going on here, it's taken care of in Bot

            logger.info { "Loaded bot" }
        } catch (e: Exception) {
            logger.error(e) { "Unable to start the bot" }
            exitProcess(1)
        }
    }
}
