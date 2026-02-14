package me.davidgomes.demo.arena

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack

class ArenaEventHandler(val arenaManager: ArenaManager) : Listener {

    val arenaJoinItem = ItemStack(Material.CLOCK).apply {
        editMeta { it.customName(Component.text("Join Arena")) }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        player.inventory.setItem(0, arenaJoinItem)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        arenaManager.leaveArena(event.player.uniqueId)
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val item = event.itemDrop.itemStack

        // TODO: not working
        if (item.type == arenaJoinItem.type && item.itemMeta?.customName() == arenaJoinItem.displayName()) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        if (arenaManager.isInArena(event.player.uniqueId)) {
            event.player.sendMessage(Component.text("You are already in the arena!"))

            return
        }

        val item = event.item ?: return

        if (!(item.type == arenaJoinItem.type && item.itemMeta?.customName() == arenaJoinItem.displayName())) return

        arenaManager.joinArena(event.player.uniqueId)
        event.player.sendMessage(Component.text("You have joined the arena!"))

        event.isCancelled = true
    }
}