package me.davidgomes.demo.map.creation

import me.davidgomes.demo.arena.Team
import me.davidgomes.demo.map.GameMap
import me.davidgomes.demo.map.MapManager
import me.davidgomes.demo.messages.*
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.logging.Logger

class MapCreationManager(
    val logger: Logger,
    val mapManager: MapManager,
    val sessions: MutableMap<Player, MapCreationSession> = mutableMapOf(),
) {
    fun createSession(
        creator: Player,
        mapName: String,
    ): MapCreationSession {
        val session =
            if (mapManager existsMapWithName mapName) {
                creator.sendMessage(EDITING_EXISTING_MAP)
                val existingTeamSpawns = mapManager.getMapByName(mapName)!!.teamSpawns

                MapCreationSession(mapName, existingTeamSpawns.toMutableMap())
            } else {
                MapCreationSession(mapName)
            }

        sessions[creator] = session

        creator.gameMode = GameMode.CREATIVE
        creator.inventory.apply {
            MapCreationItems.spawnPickers.entries.forEachIndexed { index, entry ->
                setItem(index + 1, entry.value)
            }
        }

        creator.showTitle(CREATING_MAP_TITLE)

        logger.info("Started map creation session for player ${creator.name}")

        return session
    }

    infix fun isNotInSession(player: Player): Boolean = !isInSession(player)

    infix fun isInSession(player: Player): Boolean = sessions.containsKey(player)

    fun getSession(creator: Player): MapCreationSession? = sessions[creator]

    fun abortSession(creator: Player) {
        if (!sessions.containsKey(creator)) {
            logger.warning("Tried aborting map creation for player ${creator.name} but they don't have an active session")
            return
        }

        creator.resetTitle()
        creator.sendMessage(ABORTED_MAP_CREATION)
        creator.inventory.clear()

        sessions.remove(creator)

        logger.info("Aborted session for player ${creator.name}")
    }

    fun finishSession(creator: Player): MapCreationSession? {
        val session = sessions.remove(creator)

        if (session == null) {
            logger.warning("Tried finishing map creation for player ${creator.name} but they don't have an active session!")

            return null
        }

        if (!session.isComplete()) {
            logger.warning("Tried finishing map creation for player ${creator.name} but the session is not complete")
            creator.sendMessage(Component.text("You cannot finish the map creation yet, not all spawns have been set!"))

            return null
        }

        creator.inventory.clear()
        creator.resetTitle()

        logger.info("Finished map creation session for player ${creator.name}")
        mapManager.addMap(session)

        creator.sendMessage(finishedMap(session.mapName))
        creator.server.broadcast(mapCreationBroadcast(creator.name, session.mapName))

        return session
    }

    class MapCreationSession(
        val mapName: String,
        val spawns: MutableMap<Team, Location?> = Team.entries.associateWith { null }.toMutableMap(),
    ) {
        fun isComplete(): Boolean = spawns.values.none { it == null }

        fun setSpawn(
            team: Team,
            location: Location,
        ) {
            spawns[team] = location
        }

        fun toGameMap(): GameMap {
            if (!isComplete()) {
                throw IllegalStateException("Cannot create map, not all spawns have been set!")
            }

            return GameMap(mapName, spawns.mapValues { it.value!! })
        }
    }
}
