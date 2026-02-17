package me.davidgomes.demo.map

import me.davidgomes.demo.Main
import me.davidgomes.demo.arena.model.Team
import org.bukkit.Location
import org.bukkit.configuration.InvalidConfigurationException
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import kotlin.test.*

class GameMapTest {
    private lateinit var server: ServerMock

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        MockBukkit.load(Main::class.java)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `serialize returns map with name and teamSpawns`() {
        val world = server.addSimpleWorld("test_world")
        val yellowSpawn = Location(world, 10.0, 65.0, 20.0)
        val blueSpawn = Location(world, 30.0, 65.0, 40.0)
        val teamSpawns = mapOf(Team.Yellow to yellowSpawn, Team.Blue to blueSpawn)

        val gameMap = GameMap("test_map", teamSpawns)

        val serialized = gameMap.serialize()

        assertEquals("test_map", serialized["name"])
        val serializedSpawns = serialized["teamSpawns"] as Map<String, Map<String, Any>>
        assertEquals(2, serializedSpawns.size)
        assertEquals(yellowSpawn.serialize(), serializedSpawns[Team.Yellow.name])
        assertEquals(blueSpawn.serialize(), serializedSpawns[Team.Blue.name])
    }

    @Test
    fun `deserialize throws when name is missing`() {
        val serialized =
            mapOf<String, Any>(
                "teamSpawns" to emptyMap<String, Any>(),
            )

        assertFailsWith<InvalidConfigurationException> {
            GameMap.deserialize(serialized)
        }
    }

    @Test
    fun `deserialize throws when teamSpawns is missing`() {
        val serialized =
            mapOf<String, Any>(
                "name" to "test_map",
            )

        assertFailsWith<InvalidConfigurationException> {
            GameMap.deserialize(serialized)
        }
    }

    @Test
    fun `deserialize creates GameMap from valid serialized data`() {
        val world = server.addSimpleWorld("test_world")
        val yellowSpawn = Location(world, 10.0, 65.0, 20.0)
        val blueSpawn = Location(world, 30.0, 65.0, 40.0)
        val teamSpawns = mapOf("Yellow" to yellowSpawn.serialize(), "Blue" to blueSpawn.serialize())

        val serialized =
            mapOf(
                "name" to "test_map",
                "teamSpawns" to teamSpawns,
            )

        val gameMap = GameMap.deserialize(serialized)

        assertEquals("test_map", gameMap.name)
        assertEquals(yellowSpawn, gameMap.teamSpawns[Team.Yellow])
        assertEquals(blueSpawn, gameMap.teamSpawns[Team.Blue])
    }
}
