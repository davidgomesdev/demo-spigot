package me.davidgomes.demo

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import me.davidgomes.demo.arena.ArenaEventHandler
import me.davidgomes.demo.arena.ArenaManager
import me.davidgomes.demo.arena.HeroManager
import me.davidgomes.demo.arena.HeroSelectorInventory
import me.davidgomes.demo.heroes.butcher.AnvilDropEventHandler
import me.davidgomes.demo.map.GameMap
import me.davidgomes.demo.map.MapManager
import me.davidgomes.demo.map.creation.MapCreationCommands
import me.davidgomes.demo.map.creation.MapCreationInteractions
import me.davidgomes.demo.map.creation.MapCreationManager
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.plugin.java.JavaPlugin
import utils.ExYamlConfiguration
import java.io.File

open class Main : JavaPlugin() {
    override fun onEnable() {
        ConfigurationSerialization.registerClass(GameMap::class.java)

        val heroManager = HeroManager(this, logger)
        val arenaManager = ArenaManager(this, logger, heroManager)
        val heroSelectorInventory = HeroSelectorInventory(server)

        val mapManager = MapManager(logger, getConfigFile("maps.yml"))
        val mapCreationManager = MapCreationManager(logger, mapManager)
        val mapCreationCommands = MapCreationCommands(logger, mapCreationManager)

        val commandsToRegister = arrayOf(mapCreationCommands.createMap)

        val eventHandlers = listOf(
            AnvilDropEventHandler(this, logger),
            MapCreationInteractions(mapCreationManager),
            ArenaEventHandler(logger, arenaManager, heroSelectorInventory)
        )

        eventHandlers.forEach { server.pluginManager.registerEvents(it, this)}

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commandsRegister ->
            commandsRegister.registrar().run {
                commandsToRegister.forEach(::register)
            }
        }

        logger.info("Enabled")
    }

    override fun onDisable() {
        logger.info("Disabled")
    }

    fun getConfigFile(filename: String): ExYamlConfiguration = ExYamlConfiguration(File(this.dataFolder, filename))
}
