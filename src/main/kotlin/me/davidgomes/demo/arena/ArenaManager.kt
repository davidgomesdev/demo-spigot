package me.davidgomes.demo.arena

import java.util.UUID
import java.util.logging.Logger

// TODO: NOT THREAD-SAFE, needs a mutex
class ArenaManager(
    private val logger: Logger,
    private val players: MutableMap<Team, MutableList<UUID>> =
        Team.entries.associateWith { mutableListOf<UUID>() }
            .toMutableMap()
) {

    fun joinArena(playerId: UUID): Team {
        val team = players.minBy { it.value.size }.key

        players[team]?.add(playerId) ?: players.put(team, mutableListOf(playerId))

        return team
    }

    /*
        * If the player is not in any team, it logs a warning and does nothing.
     */
    fun leaveArena(playerId: UUID) {
        val team = getTeam(playerId)

        if (team == null) {
            logger.warning("Tried removing player with ID $playerId is not in any team")
            return
        }

        players[team]?.remove(playerId)
    }

    fun isInArena(playerId: UUID): Boolean {
        return getTeam(playerId) != null
    }

    fun getTeam(playerId: UUID): Team? {
        return Team.entries.firstOrNull { players[it]?.contains(playerId) == true }
    }

    fun getTeamSize(team: Team): Int {
        return players[team]?.size ?: 0
    }

    fun getPlayersInTeam(team: Team): List<UUID> {
        return players[team]?.toList() ?: emptyList()
    }
}

enum class Team {
    Yellow, Blue
}
