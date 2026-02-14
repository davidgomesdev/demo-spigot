package me.davidgomes.demo.arena

import org.junit.jupiter.api.assertDoesNotThrow
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArenaManagerTest {

    private fun createArenaManager(): ArenaManager {
        return ArenaManager(Logger.getLogger("ArenaManagerTest"))
    }

    @Test
    fun `joinArena assigns player to a team`() {
        val arenaManager = createArenaManager()
        val playerId = UUID.randomUUID()

        val team = arenaManager.joinArena(playerId)

        assertTrue(arenaManager.isInArena(playerId))
        assertEquals(team, arenaManager.getTeam(playerId))
    }

    @Test
    fun `second player joins second team when there is already one of the first`() {
        val arenaManager = createArenaManager()

        val firstTeam = arenaManager.joinArena(UUID.randomUUID())
        val secondTeam = arenaManager.joinArena(UUID.randomUUID())

        assertEquals(Team.Yellow, firstTeam)
        assertEquals(Team.Blue, secondTeam)
    }

    @Test
    fun `joinArena assigns player to team with least players`() {
        val arenaManager = createArenaManager()

        repeat(3) {
            arenaManager.joinArena(UUID.randomUUID())
        }

        val newPlayerId = UUID.randomUUID()
        val assignedTeam = arenaManager.joinArena(newPlayerId)

        // Should be assigned to Blue since it has fewer players
        assertEquals(Team.Blue, assignedTeam)
        assertTrue(arenaManager.getPlayersInTeam(Team.Blue).contains(newPlayerId))
        assertEquals(2, arenaManager.getTeamSize(Team.Blue))
        assertEquals(2, arenaManager.getTeamSize(Team.Yellow))
    }

    @Test
    fun `leaveArena removes player from their team`() {
        val arenaManager = createArenaManager()
        val playerId = UUID.randomUUID()

        arenaManager.joinArena(playerId)
        assertTrue(arenaManager.isInArena(playerId))

        arenaManager.leaveArena(playerId)
        assertFalse(arenaManager.isInArena(playerId))
    }

    @Test
    fun `leaveArena handles player not in arena gracefully`() {
        val arenaManager = createArenaManager()
        val playerId = UUID.randomUUID()

        assertDoesNotThrow {
            arenaManager.leaveArena(playerId)
        }
        assertFalse(arenaManager.isInArena(playerId))
    }

    @Test
    fun `isInArena returns false for player not in arena`() {
        val arenaManager = createArenaManager()
        val playerId = UUID.randomUUID()

        assertFalse(arenaManager.isInArena(playerId))
    }

    @Test
    fun `isInArena returns true for player in arena`() {
        val arenaManager = createArenaManager()
        val playerId = UUID.randomUUID()

        arenaManager.joinArena(playerId)

        assertTrue(arenaManager.isInArena(playerId))
    }

    @Test
    fun `first player joins first team when no players exist`() {
        val arenaManager = createArenaManager()
        val playerId = UUID.randomUUID()

        val team = arenaManager.joinArena(playerId)

        assertEquals(Team.entries.first(), team)
    }
}

