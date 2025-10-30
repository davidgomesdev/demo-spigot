package me.davidgomes.demo

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class EvtHandler : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerJoin(evt: PlayerJoinEvent) {
        Bukkit.broadcastMessage("Welcome to the server!")
    }
}
