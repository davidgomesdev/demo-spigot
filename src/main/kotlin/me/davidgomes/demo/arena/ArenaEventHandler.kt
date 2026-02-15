package me.davidgomes.demo.arena

import me.davidgomes.demo.items.InteractableItem
import me.davidgomes.demo.messages.ALREADY_IN_ARENA
import me.davidgomes.demo.messages.JOINED_ARENA
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import utils.isNotRightClick
import java.util.logging.Logger

// TODO: remove arena join item when player joins arena, and give it back when they leave
class ArenaEventHandler(
    val logger: Logger,
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
        logger.info("Added arena join item to player '${player.name}' on join")
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (arenaManager.isInArena(event.player.uniqueId)) {
            arenaManager.leaveArena(event.player.uniqueId)
            logger.info("Removed player '${event.player.name}' from arena on quit")
        }
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        if (arenaJoinItem isNotTheSame event.itemDrop) return

        event.isCancelled = true
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.isNotRightClick()) return
        if (event.item != arenaJoinItem) return

        event.isCancelled = true

        if (arenaManager.isInArena(event.player.uniqueId)) {
            event.player.sendMessage(ALREADY_IN_ARENA)
            logger.info("Player '${event.player.name}' tried to join arena but is already in one")
            return
        }

        arenaManager.joinArena(event.player.uniqueId)
        event.player.sendMessage(JOINED_ARENA)
        logger.info("Player '${event.player.name}' joined an arena")
    }
}
