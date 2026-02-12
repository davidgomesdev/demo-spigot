package me.davidgomes.demo.arena

import java.util.UUID
import java.util.logging.Logger

class ArenaManager(val logger: Logger) {
    val players = mutableMapOf<Team, MutableList<UUID>>()

    fun joinArena(playerId: UUID): Team {
        val team = players.minByOrNull { it.value.size }?.key ?: Team.entries.first()

        players[team]?.add(playerId) ?: players.put(team, mutableListOf(playerId))

        return team
    }

    fun leaveArena(playerId: UUID) {
        val team = Team.entries.firstOrNull { players[it]?.contains(playerId) == true }

        if (team == null) {
            logger.warning("Tried removing player with ID $playerId is not in any team")
            return
        }

        players[team]?.remove(playerId)
    }
}

enum class Team {
    Yellow, Blue
}
