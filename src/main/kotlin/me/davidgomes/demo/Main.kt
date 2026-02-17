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
import org.bukkit.scheduler.BukkitRunnable
import utils.ExYamlConfiguration
import java.io.File

lateinit var plugin: Plugin

open class Main : JavaPlugin() {

    lateinit var arenaManager: ArenaManager

    init {
        plugin = this
    }

    override fun onEnable() {
        val mapManager = MapManager(logger, getConfigFile("maps.yml"))
        val arenaConfig = getConfigFile("arena.yml")

        val previousLocationManager = PreviousLocationManager(plugin, logger)
        val heroManager = HeroManager(plugin, logger)
        arenaManager = ArenaManager(plugin, logger, heroManager, mapManager, previousLocationManager, arenaConfig)

        val mapCreationManager = MapCreationManager(logger, mapManager, arenaManager)
        val mapCreationCommands = MapCreationCommands(logger, mapCreationManager)

        ConfigurationSerialization.registerClass(GameMap::class.java)
        val heroSelectorInventory = HeroSelectorInventory(server)

        val commandsToRegister = arrayOf(mapCreationCommands.createMap)

        val eventHandlers =
            listOf(
                AnvilDropEventHandler(plugin, logger),
                MapCreationInteractions(logger, mapCreationManager),
                ArenaEventHandler(logger, arenaManager, heroSelectorInventory),
            )

        eventHandlers.forEach { server.pluginManager.registerEvents(it, plugin) }

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commandsRegister ->
            commandsRegister.registrar().run {
                commandsToRegister.forEach(::register)
            }
        }

        logger.info("Enabled")

        // Delay is to prevent this plugin from loading before Multiverse, which was leading to
        // issues with loading maps from the config.
        object : BukkitRunnable() {
            override fun run() {
                mapManager.reloadMaps()
            }
        }.runTaskLater(this, 0L)
    }

    override fun onDisable() {
        logger.info("Disabled")

        arenaManager.getPlayersInArena().forEach(arenaManager::leaveArena)
    }

    fun getConfigFile(filename: String): ExYamlConfiguration {
        val configFile = File(this.dataFolder, filename)

        if (getResource(filename) != null && !configFile.exists()) {
            saveResource(filename, false)
            logger.info("Saved default config file '$filename' to data folder")
        }

        return ExYamlConfiguration(configFile)
    }
}
