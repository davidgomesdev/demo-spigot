package me.davidgomes.demo.arena

import me.davidgomes.demo.isRightClick
import me.davidgomes.demo.items.InteractableItem
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

// TODO: remove arena join item when player joins arena, and give it back when they leave
class ArenaEventHandler(
    val arenaManager: ArenaManager,
) : Listener {
    val arenaJoinItem =
        InteractableItem(
            material = Material.DIAMOND_SWORD,
            name = "Join Arena",
        )

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        player.inventory.setItem(0, arenaJoinItem)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (arenaManager.isInArena(event.player.uniqueId)) {
            arenaManager.leaveArena(event.player.uniqueId)
        }
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        if (arenaJoinItem isNotTheSame event.itemDrop) return

        event.isCancelled = true
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (!event.isRightClick()) return
        if (event.item != arenaJoinItem) return

        event.isCancelled = true

        if (arenaManager.isInArena(event.player.uniqueId)) {
            event.player.sendMessage(Component.text("You are already in the arena!"))
            return
        }

        arenaManager.joinArena(event.player.uniqueId)
        event.player.sendMessage(Component.text("You have joined the arena!"))
    }
}
