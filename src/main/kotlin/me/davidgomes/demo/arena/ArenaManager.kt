package me.davidgomes.demo.arena

import me.davidgomes.demo.items.InteractableItem
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.*
import java.util.logging.Logger

val arenaJoinItem =
    InteractableItem(
        material = Material.DIAMOND_SWORD,
        name = "Join Arena",
    )

// TODO: NOT THREAD-SAFE, needs a mutex
class ArenaManager(
    private val logger: Logger,
    private val players: MutableMap<Team, MutableList<UUID>> =
        Team.entries
            .associateWith { mutableListOf<UUID>() }
            .toMutableMap(),
) {
    fun addItemJoinArena(player: Player) {
        player.inventory.apply {
            clear()
            setItem(0, arenaJoinItem)
        }
    }

    fun joinArena(player: Player): Team {
        val playerId = player.uniqueId
        val team = players.minBy { it.value.size }.key

        players[team]?.add(playerId) ?: players.put(team, mutableListOf(playerId))
        player.inventory.clear()

        return team
    }

    /*
     * If the player is not in any team, it logs a warning and does nothing.
     */
    fun leaveArena(player: Player) {
        val playerId = player.uniqueId
        val team = getTeam(playerId)

        if (team == null) {
            logger.warning("Tried removing player with ID $playerId but they are not in any team")
            return
        }

        players[team]?.remove(playerId)
        addItemJoinArena(player)
    }

    fun isInArena(playerId: UUID): Boolean = getTeam(playerId) != null

    fun getTeam(playerId: UUID): Team? = Team.entries.firstOrNull { players[it]?.contains(playerId) == true }

    fun getTeamSize(team: Team): Int = players[team]?.size ?: 0

    fun getPlayersInTeam(team: Team): List<UUID> = players[team]?.toList() ?: emptyList()
}

enum class Team(
    val spawnItemMaterial: Material,
) {
    Yellow(Material.YELLOW_CANDLE),
    Blue(Material.BLUE_CANDLE),
    ;

    companion object {
        val count = entries.count()
    }
}
