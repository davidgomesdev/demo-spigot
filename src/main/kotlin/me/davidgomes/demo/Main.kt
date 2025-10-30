package me.davidgomes.demo

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class Demo : JavaPlugin() {
    override fun onEnable() {
        Bukkit.getLogger().info("Enabled " + this.name)

        server.pluginManager.registerEvents(EvtHandler(), this)
    }

    override fun onDisable() {
        Bukkit.getLogger().info("Disabled " + this.name)
    }
}
