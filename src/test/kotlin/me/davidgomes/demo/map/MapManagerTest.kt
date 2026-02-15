package me.davidgomes.demo.map

import io.mockk.spyk
import io.mockk.verify
import me.davidgomes.demo.Main
import me.davidgomes.demo.arena.Team
import me.davidgomes.demo.createTempConfig
import me.davidgomes.demo.map.creation.MapCreationManager
import org.bukkit.Location
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import utils.ExYamlConfiguration
import java.util.logging.Logger
import kotlin.test.*

class MapManagerTest {
    private lateinit var server: ServerMock
    private lateinit var logger: Logger
    private lateinit var config: ExYamlConfiguration

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        MockBukkit.load(Main::class.java)
        logger = Logger.getLogger("MapManagerTest")
        config = spyk(createTempConfig())
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
        config.file.delete()
    }

    @Test
    fun `existsMapWithName returns false when no maps exist`() {
        val manager = MapManager(logger, config)

        assertFalse(manager existsMapWithName "non_existent")
    }

    @Test
    fun `existsMapWithName returns true when map with name exists`() {
        val world = server.addSimpleWorld("test_world")
        val teamSpawns = mapOf(
            Team.Yellow to Location(world, 0.0, 0.0, 0.0),
            Team.Blue to Location(world, 10.0, 0.0, 0.0)
        )
        val gameMap = GameMap("existing_map", teamSpawns)

        config.setAndSave("maps", listOf(gameMap))

        val manager = MapManager(logger, config)

        assertTrue(manager existsMapWithName "existing_map")
    }

    @Test
    fun `reloadMaps returns empty list when no maps in config`() {
        val manager = MapManager(logger, config)

        val maps = manager.reloadMaps()

        assertEquals(0, maps.size)
    }

    @Test
    fun `reloadMaps loads maps from config`() {
        val world = server.addSimpleWorld("test_world")
        val teamSpawns = mapOf(
            Team.Yellow to Location(world, 0.0, 0.0, 0.0),
            Team.Blue to Location(world, 10.0, 0.0, 0.0)
        )
        val gameMap = GameMap("test_map", teamSpawns)

        config.setAndSave("maps", listOf(gameMap))

        val manager = MapManager(logger, config)

        assertEquals(1, manager.getAllMaps().size)
        assertEquals("test_map", manager.getAllMaps().first().name)
    }

    @Test
    fun `addMap saves map to config`() {
        val world = server.addSimpleWorld("test_world")
        val manager = MapManager(logger, config)

        val session = MapCreationManager.MapCreationSession("new_map")
        session.setSpawn(Team.Yellow, Location(world, 0.0, 0.0, 0.0))
        session.setSpawn(Team.Blue, Location(world, 10.0, 0.0, 0.0))

        val addedMap = manager.addMap(session)

        assertEquals("new_map", addedMap.name)
        verify { config.setAndSave("maps", any()) }
    }

    @Test
    fun `addMap updates maps list after adding`() {
        val world = server.addSimpleWorld("test_world")
        val manager = MapManager(logger, config)

        val session = MapCreationManager.MapCreationSession("new_map")
        session.setSpawn(Team.Yellow, Location(world, 0.0, 0.0, 0.0))
        session.setSpawn(Team.Blue, Location(world, 10.0, 0.0, 0.0))

        manager.addMap(session)

        assertEquals(1, manager.getAllMaps().size)
        assertTrue(manager existsMapWithName "new_map")
    }
}

