package me.davidgomes.demo.arena

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack

// TODO: remove arena join item when player joins arena, and give it back when they leave
class ArenaEventHandler(val arenaManager: ArenaManager) : Listener {

    val joinItemName = Component.text("Join Arena")

    // TODO: make a class for interactable items
    val arenaJoinItem = ItemStack(Material.CLOCK).apply {
        editMeta { it.customName(joinItemName) }
    }

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
        val item = event.itemDrop.itemStack

        val customName = item.itemMeta?.customName() ?: return
        if (customName !is TextComponent) return

        if (!(item.type == arenaJoinItem.type && customName == joinItemName)) return

        event.isCancelled = true
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        val item = event.item ?: return

        val customName = item.itemMeta?.customName() ?: return
        if (customName !is TextComponent) return

        if (!(item.type == arenaJoinItem.type && customName == joinItemName)) return

        event.isCancelled = true

        if (arenaManager.isInArena(event.player.uniqueId)) {
            event.player.sendMessage(Component.text("You are already in the arena!"))
            return
        }

        arenaManager.joinArena(event.player.uniqueId)
        event.player.sendMessage(Component.text("You have joined the arena!"))
    }
}