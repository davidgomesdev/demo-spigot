package me.davidgomes.demo.map.creation

import me.davidgomes.demo.arena.model.Team
import me.davidgomes.demo.messages.CANNOT_FINISH_YET_MESSAGE
import me.davidgomes.demo.messages.FINISHED_MAP_CREATION
import me.davidgomes.demo.messages.NOT_DROPPABLE_WHILE_CREATING_MESSAGE
import me.davidgomes.demo.messages.NOT_IN_SESSION_MESSAGE
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import utils.isNotRightClick
import java.util.logging.Logger

class MapCreationInteractions(
    private val logger: Logger,
    private val manager: MapCreationManager,
) : Listener {
    @EventHandler
    fun onPlayerSetSpawn(event: PlayerInteractEvent) {
        if (event.isNotRightClick()) return

        val creator = event.player

        val team = MapCreationItems.getTeamFromSpawnPicker(event.item ?: return) ?: return

        if (manager isNotInSession creator) {
            creator.sendMessage(NOT_IN_SESSION_MESSAGE)
            return
        }

        val session = manager.getSession(creator) ?: return

        event.isCancelled = true

        session.setSpawn(team, creator.location)
        creator.sendMessage("You've just set the spawn for team ${team.name} at your current location.")

        if (session.isComplete()) {
            logger.info(
                "Map creation session for player ${creator.name} is now ready to complete, " +
                    "all spawns have been set.",
            )
            creator.inventory.run {
                val finishCreationItemPosition = Team.count
                if (getItem(finishCreationItemPosition) != null) return

                // Add next to the spawn pickers
                setItem(finishCreationItemPosition, MapCreationItems.finishCreation)
                creator.sendMessage("You can already finish the map creation, all spawns have been set.")
            }
        }
    }

    @EventHandler
    fun onPlayerFinishCreation(event: PlayerInteractEvent) {
        if (event.isNotRightClick()) return
        if (MapCreationItems.finishCreation isNotTheSame event.item) return

        val creator = event.player

        if (manager isNotInSession creator) {
            creator.sendMessage(NOT_IN_SESSION_MESSAGE)
            return
        }

        val session = manager.getSession(creator) ?: return

        if (!session.isComplete()) {
            creator.sendMessage(CANNOT_FINISH_YET_MESSAGE)
            return
        }

        manager.finishSession(creator)

        creator.sendMessage(FINISHED_MAP_CREATION)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (manager isNotInSession event.player) return

        manager.abortSession(event.player)
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (manager isNotInSession event.player) return

        manager.abortSession(event.player)
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerInteractEvent) {
        if (manager isNotInSession event.player) return
        if (MapCreationItems.finishCreation isNotTheSame event.item) return

        val session = manager.getSession(event.player) ?: return

        if (session.isComplete()) {
            event.player.sendMessage(NOT_DROPPABLE_WHILE_CREATING_MESSAGE)
        }

        event.isCancelled = true
    }
}
