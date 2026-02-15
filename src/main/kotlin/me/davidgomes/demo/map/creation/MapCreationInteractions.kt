package me.davidgomes.demo.map.creation

import me.davidgomes.demo.arena.Team
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import utils.isNotLeftClick

class MapCreationInteractions(
    private val manager: MapCreationManager,
) {

    @EventHandler
    fun onPlayerSetSpawn(event: PlayerInteractEvent) {
        if (event.isNotLeftClick()) return

        val creator = event.player

        if (manager isNotInSession creator) return

        val team = MapCreationItems.getTeamFromSpawnPicker(event.item ?: return) ?: return
        val session = manager.getSession(creator) ?: return

        event.isCancelled = true

        creator.location.direction
        session.setSpawn(team, creator.location)
        creator.sendMessage("You've just set the spawn for team ${team.name} at your current location.")

        if (session.isComplete()) {
            creator.inventory.run {
                if (getItem(Team.count) != null) return

                // Add next to the spawn pickers
                setItem(Team.count, MapCreationItems.finishCreation)
                creator.sendMessage("You can already finish the map creation, all spawns have been set.")
            }
        }
    }

    @EventHandler
    fun onPlayerFinishCreation(event: PlayerInteractEvent) {
        if (event.isNotLeftClick()) return
        if (manager isNotInSession event.player) return
        if (MapCreationItems.finishCreation isNotTheSame event.item) return

        val session = manager.getSession(event.player) ?: return

        if (!session.isComplete()) {
            event.player.sendMessage(Component.text("You cannot finish the map creation yet, not all spawns have been set!"))
            return
        }

        manager.finishSession(event.player)

        event.player.sendMessage(Component.text("Finished map creation!"))
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerInteractEvent) {
        if (manager isNotInSession event.player) return

        manager.abortSession(event.player)
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerInteractEvent) {
        if (manager isNotInSession event.player) return

        manager.abortSession(event.player)
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerInteractEvent) {
        if (manager isNotInSession event.player) return
        if (MapCreationItems.finishCreation isNotTheSame event.item) return

        val session = manager.getSession(event.player) ?: return

        if (session.isComplete()) {
            event.player.sendMessage(Component.text("You cannot drop the finish creation item!"))
        }

        event.isCancelled = true
    }
}