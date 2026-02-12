package me.davidgomes.demo

import me.davidgomes.demo.heroes.butcher.AnvilDropEventHandler
import org.bukkit.plugin.java.JavaPlugin

class Demo : JavaPlugin() {
    override fun onEnable() {
        logger.info("Enabled")
        server.pluginManager.registerEvents(AnvilDropEventHandler(this, logger), this)
    }

    override fun onDisable() {
        logger.info("Disabled")
    }
}
