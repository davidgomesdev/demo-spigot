package me.davidgomes.demo.arena

import org.junit.jupiter.api.assertDoesNotThrow
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import java.util.logging.Logger
import kotlin.test.*

class ArenaManagerTest {
    lateinit var arenaManager: ArenaManager
    lateinit var server: ServerMock

    @BeforeTest
    fun setUp() {
        MockBukkit.mock()
        server = ServerMock()
        arenaManager = ArenaManager(Logger.getLogger("ArenaManagerTest"))
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `joinArena assigns player to a team`() {
        val player = server.addPlayer()
        val team = arenaManager.joinArena(player)

        assertTrue(arenaManager.isInArena(player.uniqueId))
        assertEquals(team, arenaManager.getTeam(player.uniqueId))
    }

    @Test
    fun `second player joins second team when there is already one of the first`() {
        val firstTeam = arenaManager.joinArena(server.addPlayer())
        val secondTeam = arenaManager.joinArena(server.addPlayer())

        assertEquals(Team.Yellow, firstTeam)
        assertEquals(Team.Blue, secondTeam)
    }

    @Test
    fun `joinArena assigns player to team with least players`() {
        repeat(3) {
            arenaManager.joinArena(server.addPlayer())
        }

        val newPlayer = server.addPlayer()
        val assignedTeam = arenaManager.joinArena(newPlayer)

        // Should be assigned to Blue since it has fewer players
        assertEquals(Team.Blue, assignedTeam)
        assertTrue(arenaManager.getPlayersInTeam(Team.Blue).contains(newPlayer.uniqueId))
        assertEquals(2, arenaManager.getTeamSize(Team.Blue))
        assertEquals(2, arenaManager.getTeamSize(Team.Yellow))
    }

    @Test
    fun `leaveArena removes player from their team`() {
        val player = server.addPlayer()

        arenaManager.joinArena(player)
        assertTrue(arenaManager.isInArena(player.uniqueId))

        arenaManager.leaveArena(player)
        assertFalse(arenaManager.isInArena(player.uniqueId))
    }

    @Test
    fun `leaveArena handles player not in arena gracefully`() {
        val player = server.addPlayer()

        assertDoesNotThrow {
            arenaManager.leaveArena(player)
        }
        assertFalse(arenaManager.isInArena(player.uniqueId))
    }

    @Test
    fun `isInArena returns false for player not in arena`() {
        val player = server.addPlayer()

        assertFalse(arenaManager.isInArena(player.uniqueId))
    }

    @Test
    fun `isInArena returns true for player in arena`() {
        val player = server.addPlayer()

        arenaManager.joinArena(player)

        assertTrue(arenaManager.isInArena(player.uniqueId))
    }

    @Test
    fun `first player joins first team when no players exist`() {
        val player = server.addPlayer()
        val team = arenaManager.joinArena(player)

        assertEquals(Team.entries.first(), team)
    }
}
