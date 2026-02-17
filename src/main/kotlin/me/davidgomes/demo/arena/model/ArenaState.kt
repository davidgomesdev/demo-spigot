package me.davidgomes.demo.arena.model

import me.davidgomes.demo.map.TeamSpawns
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.concurrent.atomic.AtomicInteger

const val DEFAULT_SCORE_GOAL = 10

@Suppress("unused")
sealed class ArenaState(
    val isOnGoing: Boolean = true,
) {
    // TODO: add a lobby "spawn"
    data object Lobby : ArenaState(false)

    class OnGoingTeamDeathMatch(
        val teamSpawns: TeamSpawns,
        val scoreGoal: Int = DEFAULT_SCORE_GOAL,
        val scoreboard: Map<Team, AtomicInteger> = Team.entries.associateWith { AtomicInteger(0) },
    ) : ArenaState() {
        /**
         * @return the winning team if the score goal was reached, null otherwise
         */
        fun scoreKill(executorTeam: Team): Team? {
            val executorTeamScore = scoreboard[executorTeam]!!

            val newScore = executorTeamScore.incrementAndGet()

            if (newScore < scoreGoal) return null

            return executorTeam
        }
    }

    data class OnGoingFreeForAll(
        val spawns: List<Location>,
        val scoreboard: Map<Player, AtomicInteger>,
        val scoreGoal: Int,
    ) : ArenaState()

    /**
     * @param winningTeam First to reach the score goal or most kills (two teams can tie in score, but the first one to kill the last wins)
     */
    data class EndedTeamDeathMatch(
        val winningTeam: Team,
    ) : ArenaState(false)

    /**
     * @param winner First to reach the score goal or most kills (two players can tie in score, but the first one to kill the last wins)
     */
    data class EndedFreeForAll(
        val winner: Player,
    ) : ArenaState(false)
}
