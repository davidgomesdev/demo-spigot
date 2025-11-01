package me.davidgomes.demo

import me.davidgomes.demo.heroes.butcher.ButcherEventHandler
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger

lateinit var log: Logger
lateinit var plugin: JavaPlugin

class Demo : JavaPlugin() {
    override fun onEnable() {
        log = Bukkit.getLogger()
        plugin = this
        server.broadcastMessage("Minigame loaded and ready to play!")
        logger.info("Enabled")

        server.pluginManager.registerEvents(ButcherEventHandler(), this)
    }

    override fun onDisable() {
        logger.info("Disabled")
    }
}
