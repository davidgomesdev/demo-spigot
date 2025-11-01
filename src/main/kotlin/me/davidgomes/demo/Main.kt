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
        logger.info("Enabled " + this.name)

        server.pluginManager.registerEvents(ButcherEventHandler(), this)
    }

    override fun onDisable() {
        logger.info("Disabled " + this.name)
    }
}
