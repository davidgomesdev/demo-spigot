package me.davidgomes.demo.arena

import org.bukkit.Material
import java.util.*
import java.util.logging.Logger

// TODO: NOT THREAD-SAFE, needs a mutex
class ArenaManager(
    private val logger: Logger,
    private val players: MutableMap<Team, MutableList<UUID>> =
        Team.entries
            .associateWith { mutableListOf<UUID>() }
            .toMutableMap(),
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
            logger.warning("Tried removing player with ID $playerId but they are not in any team")
            return
        }

        players[team]?.remove(playerId)
    }

    fun isInArena(playerId: UUID): Boolean = getTeam(playerId) != null

    fun getTeam(playerId: UUID): Team? = Team.entries.firstOrNull { players[it]?.contains(playerId) == true }

    fun getTeamSize(team: Team): Int = players[team]?.size ?: 0

    fun getPlayersInTeam(team: Team): List<UUID> = players[team]?.toList() ?: emptyList()
}

enum class Team(val spawnItemMaterial: Material) {
    Yellow(Material.YELLOW_CANDLE),
    Blue(Material.BLUE_CANDLE);

    companion object {
        val count = entries.count()
    }
}
