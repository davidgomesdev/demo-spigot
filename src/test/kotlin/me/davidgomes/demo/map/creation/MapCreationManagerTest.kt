package me.davidgomes.demo.map.creation

import io.mockk.spyk
import io.mockk.verify
import me.davidgomes.demo.Main
import me.davidgomes.demo.arena.model.Team
import me.davidgomes.demo.createPlayer
import me.davidgomes.demo.createTempConfig
import me.davidgomes.demo.map.MapManager
import org.bukkit.GameMode
import org.bukkit.Location
import org.junit.jupiter.api.Nested
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import java.util.logging.Logger
import kotlin.test.*

class MapCreationManagerTest {
    private lateinit var server: ServerMock
    private lateinit var logger: Logger
    private lateinit var mapManager: MapManager
    private lateinit var manager: MapCreationManager

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        MockBukkit.load(Main::class.java)

        logger = Logger.getLogger("MapCreationManagerTest")
        mapManager = spyk(MapManager(logger, createTempConfig()))
        manager = MapCreationManager(logger, mapManager)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `isNotInSession returns true when player has no session`() {
        val player = server.addPlayer()

        assertTrue(manager isNotInSession player)
    }

    @Test
    fun `isNotInSession returns false when player has session`() {
        val player = server.addPlayer()

        manager.createSession(player, "test_map")

        assertFalse(manager isNotInSession player)
    }

    @Test
    fun `getSession returns null when player has no session`() {
        val player = server.addPlayer()

        assertNull(manager.getSession(player))
    }

    @Test
    fun `getSession returns session when player has one`() {
        val player = server.addPlayer()

        manager.createSession(player, "test_map")

        val session = manager.getSession(player)

        assertNotNull(session)
        assertEquals("test_map", session.mapName)
    }

    @Nested
    inner class CreationSession {
        @Test
        fun `returns existing map when map name already exists`() {
            val player = createPlayer(server)

            manager.createSession(player, "new_map").also { session ->
                Team.entries.associateWith { player.location }.forEach { (team, location) ->
                    session.setSpawn(team, location)
                }
            }
            manager.finishSession(player)

            assertTrue(mapManager existsMapWithName "new_map")

            val session = manager.createSession(player, "new_map")

            assertEquals("new_map", session.mapName)
            session.spawns.forEach { (_, location) ->
                assertNotNull(location)
                assertEquals(player.location, location)
            }
        }

        @Test
        fun `creates session and adds spawn pickers to inventory`() {
            val player = spyk(server.addPlayer())

            val session = manager.createSession(player, "new_map")

            assertNotNull(session)
            assertEquals("new_map", session.mapName)
            assertEquals(GameMode.CREATIVE, player.gameMode)
        }

        @Test
        fun `adds player to sessions map`() {
            val player = server.addPlayer()

            manager.createSession(player, "new_map")

            assertFalse(manager isNotInSession player)
        }
    }

    @Nested
    inner class AbortSession {
        @Test
        fun `removes player from sessions and clears inventory`() {
            val player = createPlayer(server)

            manager.createSession(player, "test_map")
            manager.abortSession(player)

            assertTrue(manager isNotInSession player)
            assertTrue(player.inventory.isEmpty)
        }

        @Test
        fun `does nothing when player has no session`() {
            val player = spyk(server.addPlayer())

            // Should not throw
            manager.abortSession(player)

            assertTrue(manager isNotInSession player)
        }
    }

    @Nested
    inner class FinishSession {
        @Test
        fun `returns null when player has no session`() {
            val player = server.addPlayer()
            val result = manager.finishSession(player)

            assertNull(result)
        }

        @Test
        fun `returns null when session is incomplete`() {
            val player = server.addPlayer()

            manager.createSession(player, "test_map")

            val result = manager.finishSession(player)

            assertNull(result)
        }

        @Test
        fun `completes when all spawns are set`() {
            val world = server.addSimpleWorld("test_world")
            val player = createPlayer(server)

            manager.createSession(player, "test_map")

            val session = manager.getSession(player)!!

            session.setSpawn(Team.Yellow, Location(world, 0.0, 0.0, 0.0))
            session.setSpawn(Team.Blue, Location(world, 10.0, 0.0, 0.0))

            val result = manager.finishSession(player)

            assertNotNull(result)
            assertFalse(manager isInSession player)
            assertTrue(player.inventory.isEmpty)
            verify { mapManager.addMap(session) }
        }
    }

    @Nested
    inner class Session {
        @Test
        fun `isComplete returns false when no spawns set`() {
            val session = MapCreationManager.MapCreationSession("test_map")

            assertFalse(session.isComplete())
        }

        @Test
        fun `isComplete returns false when only some spawns set`() {
            val world = server.addSimpleWorld("test_world")
            val session = MapCreationManager.MapCreationSession("test_map")
            session.setSpawn(Team.Yellow, Location(world, 0.0, 0.0, 0.0))

            assertFalse(session.isComplete())
        }

        @Test
        fun `isComplete returns true when all spawns set`() {
            val world = server.addSimpleWorld("test_world")
            val session = MapCreationManager.MapCreationSession("test_map")
            session.setSpawn(Team.Yellow, Location(world, 0.0, 0.0, 0.0))
            session.setSpawn(Team.Blue, Location(world, 10.0, 0.0, 0.0))

            assertTrue(session.isComplete())
        }

        @Test
        fun `setSpawn updates spawn for team`() {
            val world = server.addSimpleWorld("test_world")
            val session = MapCreationManager.MapCreationSession("test_map")
            val location = Location(world, 5.0, 10.0, 15.0)

            session.setSpawn(Team.Yellow, location)

            assertEquals(location, session.spawns[Team.Yellow])
        }

        @Test
        fun `toGameMap throws when session is incomplete`() {
            val session = MapCreationManager.MapCreationSession("test_map")

            assertFailsWith<IllegalStateException> {
                session.toGameMap()
            }
        }

        @Test
        fun `toGameMap creates GameMap when complete`() {
            val world = server.addSimpleWorld("test_world")
            val session = MapCreationManager.MapCreationSession("test_map")
            val yellowLocation = Location(world, 0.0, 0.0, 0.0)
            val blueLocation = Location(world, 10.0, 0.0, 0.0)
            session.setSpawn(Team.Yellow, yellowLocation)
            session.setSpawn(Team.Blue, blueLocation)

            val gameMap = session.toGameMap()

            assertEquals("test_map", gameMap.name)
            assertEquals(yellowLocation, gameMap.teamSpawns[Team.Yellow])
            assertEquals(blueLocation, gameMap.teamSpawns[Team.Blue])
        }
    }
}
