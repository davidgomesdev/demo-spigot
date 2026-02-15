package me.davidgomes.demo.arena

import me.davidgomes.demo.messages.ALREADY_IN_ARENA
import me.davidgomes.demo.messages.JOINED_ARENA
import me.davidgomes.demo.messages.NOT_ENOUGH_PLAYERS_TO_START
import me.davidgomes.demo.messages.NOT_IN_ARENA
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import utils.isNotRightClick
import java.util.logging.Logger

class ArenaEventHandler(
    val logger: Logger,
    val arenaManager: ArenaManager,
) : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        arenaManager.addItemToJoinArena(player)
        logger.info("Added arena join item to player '${player.name}' on join")
    }

    @EventHandler
    fun onPlayerInteractWithArenaJoin(event: PlayerInteractEvent) {
        if (event.isNotRightClick()) return
        if (event.item != arenaJoinItem) return

        event.isCancelled = true

        if (arenaManager isInArena event.player) {
            event.player.sendMessage(ALREADY_IN_ARENA)
            logger.info("Player '${event.player.name}' tried to join arena but is already in one")
            return
        }

        arenaManager.joinArena(event.player)
        event.player.sendMessage(JOINED_ARENA)
        logger.info("Player '${event.player.name}' joined an arena")
    }

    @EventHandler
    fun onPlayerInteractWithArenaStart(event: PlayerInteractEvent) {
        if (event.isNotRightClick()) return
        if (event.item != arenaStartItem) return

        event.isCancelled = true

        if (!(arenaManager isInArena event.player)) {
            event.player.sendMessage(NOT_IN_ARENA)
            logger.info("Player '${event.player.name}' tried to start arena but is not in one")
            return
        }

        if (!arenaManager.isReadyToStart()) {
            event.player.sendMessage(NOT_ENOUGH_PLAYERS_TO_START)
            logger.info("Player '${event.player.name}' tried to start arena but there are not enough players")
            return
        }

        arenaManager.startArena(GameType.TeamDeathMatch)
        logger.info("Player '${event.player.name}' started the arena")
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (!arenaManager.isInArena(event.entity)) return
        if (!arenaManager.isMatchOnGoing()) return

        val damageEntity = event.damageSource.causingEntity

        if (damageEntity is Player) {
            arenaManager.onPlayerKilledByPlayer(event.entity, damageEntity)
            logger.info("Player '${event.entity.name}' was killed by player '${damageEntity.name}' in an arena match")
        } else if (damageEntity != null) {
            arenaManager.onPlayerKilledByEntity(event.entity, damageEntity)
            logger.info("Player '${event.entity.name}' was killed by an entity ")
        } else {
            logger.info("Player '${event.entity.name}' was killed by an unknown cause in an arena match (${event.damageSource})")
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (arenaManager.isInArena(event.player)) {
            arenaManager.leaveArena(event.player)
            logger.info("Removed player '${event.player.name}' from arena on quit")
        }
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        if (arenaJoinItem isNotTheSame event.itemDrop && arenaStartItem isNotTheSame event.itemDrop) return

        event.isCancelled = true
    }
}
