package me.davidgomes.demo

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import me.davidgomes.demo.arena.ArenaEventHandler
import me.davidgomes.demo.arena.ArenaManager
import me.davidgomes.demo.arena.PreviousLocationManager
import me.davidgomes.demo.arena.hero.selection.HeroManager
import me.davidgomes.demo.arena.hero.selection.HeroSelectorInventory
import me.davidgomes.demo.heroes.butcher.AnvilDropEventHandler
import me.davidgomes.demo.map.GameMap
import me.davidgomes.demo.map.MapManager
import me.davidgomes.demo.map.creation.MapCreationCommands
import me.davidgomes.demo.map.creation.MapCreationInteractions
import me.davidgomes.demo.map.creation.MapCreationManager
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import utils.ExYamlConfiguration
import java.io.File

lateinit var plugin: Plugin

open class Main : JavaPlugin() {
    val mapManager = MapManager(logger, getConfigFile("maps.yml"))
    val arenaConfig = getConfigFile("arena.yml")
    val mapCreationManager = MapCreationManager(logger, mapManager)
    val mapCreationCommands = MapCreationCommands(logger, mapCreationManager)

    val previousLocationManager = PreviousLocationManager(this, logger)
    val heroManager = HeroManager(this, logger)
    val arenaManager = ArenaManager(this, logger, heroManager, mapManager, previousLocationManager, arenaConfig)

    init {
        plugin = this
    }

    override fun onEnable() {
        ConfigurationSerialization.registerClass(GameMap::class.java)
        val heroSelectorInventory = HeroSelectorInventory(server)

        val commandsToRegister = arrayOf(mapCreationCommands.createMap)

        val eventHandlers =
            listOf(
                AnvilDropEventHandler(this, logger),
                MapCreationInteractions(mapCreationManager),
                ArenaEventHandler(logger, arenaManager, heroSelectorInventory),
            )

        eventHandlers.forEach { server.pluginManager.registerEvents(it, this) }

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commandsRegister ->
            commandsRegister.registrar().run {
                commandsToRegister.forEach(::register)
            }
        }

        logger.info("Enabled")
    }

    override fun onDisable() {
        logger.info("Disabled")

        teleportPlayersBackToOriginalLocation()
    }

    private fun teleportPlayersBackToOriginalLocation() {
        arenaManager
            .getPlayersInArena()
            .associateBy(previousLocationManager::getSavedLocation)
            .forEach { (previousLocation, player) ->
                if (previousLocation == null) {
                    logger.warning("Player '${player.name}' does not have a previous location saved, skipping teleport")
                    return@forEach
                }

                player.teleport(previousLocation)
            }
    }

    fun getConfigFile(filename: String): ExYamlConfiguration {
        if (getResource(filename) != null) {
            saveResource(filename, false)
            logger.info("Saved default config file '$filename' to data folder")
        }
        return ExYamlConfiguration(File(this.dataFolder, filename))
    }
}
