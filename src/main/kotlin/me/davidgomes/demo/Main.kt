package me.davidgomes.demo

import me.davidgomes.demo.arena.ArenaEventHandler
import me.davidgomes.demo.arena.ArenaManager
import me.davidgomes.demo.heroes.butcher.AnvilDropEventHandler
import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class Main : JavaPlugin() {
    override fun onEnable() {
        val arenaManager = ArenaManager(logger)

        server.pluginManager.registerEvents(AnvilDropEventHandler(this, logger), this)
        server.pluginManager.registerEvents(ArenaEventHandler(arenaManager), this)

        logger.info("Enabled")
    }

    override fun onDisable() {
        logger.info("Disabled")
    }
}
