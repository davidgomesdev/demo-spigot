package me.davidgomes.demo.arena

import me.davidgomes.demo.Main
import me.davidgomes.demo.pdc.LocationDataType
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import java.util.logging.Logger
import kotlin.test.*

class PreviousLocationManagerTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: Plugin
    private lateinit var logger: Logger
    private lateinit var previousLocationManager: PreviousLocationManager

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(Main::class.java)
        logger = Logger.getLogger("PreviousLocationManagerTest")
        previousLocationManager = PreviousLocationManager(plugin, logger)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `saveLocation stores location in player persistent data`() {
        val world = server.addSimpleWorld("test_world")
        val player = server.addPlayer()
        val location = Location(world, 10.0, 64.0, 20.0, 45.0f, -15.0f)
        player.teleport(location)

        previousLocationManager.saveLocation(player)

        val storedLocation =
            player.persistentDataContainer.get(
                NamespacedKey(plugin, "previous_location"),
                LocationDataType,
            )

        assertNotNull(storedLocation)
        assertEquals(location.world, storedLocation.world)
        assertEquals(location.x, storedLocation.x)
        assertEquals(location.y, storedLocation.y)
        assertEquals(location.z, storedLocation.z)
        assertEquals(location.yaw, storedLocation.yaw)
        assertEquals(location.pitch, storedLocation.pitch)
    }

    @Test
    fun `getSavedLocation retrieves location from player persistent data`() {
        val world = server.addSimpleWorld("test_world")
        val player = server.addPlayer()
        val location = Location(world, 15.5, 70.0, -25.5, 90.0f, 0.0f)
        player.teleport(location)

        previousLocationManager.saveLocation(player)
        val retrievedLocation = previousLocationManager.getSavedLocation(player)

        assertNotNull(retrievedLocation)
        assertEquals(location.world, retrievedLocation.world)
        assertEquals(location.x, retrievedLocation.x)
        assertEquals(location.y, retrievedLocation.y)
        assertEquals(location.z, retrievedLocation.z)
        assertEquals(location.yaw, retrievedLocation.yaw)
        assertEquals(location.pitch, retrievedLocation.pitch)
    }

    @Test
    fun `getSavedLocation returns null when player has no saved location`() {
        val player = server.addPlayer()

        val location = previousLocationManager.getSavedLocation(player)

        assertNull(location)
    }

    @Test
    fun `saveLocation overwrites previous saved location`() {
        val world = server.addSimpleWorld("test_world")
        val player = server.addPlayer()

        val firstLocation = Location(world, 10.0, 64.0, 10.0)
        player.teleport(firstLocation)
        previousLocationManager.saveLocation(player)

        val secondLocation = Location(world, 100.0, 70.0, 100.0)
        player.teleport(secondLocation)
        previousLocationManager.saveLocation(player)

        val retrievedLocation = previousLocationManager.getSavedLocation(player)

        assertNotNull(retrievedLocation)
        assertEquals(secondLocation.x, retrievedLocation.x)
        assertEquals(secondLocation.y, retrievedLocation.y)
        assertEquals(secondLocation.z, retrievedLocation.z)
    }

    @Test
    fun `saveLocation handles negative coordinates`() {
        val world = server.addSimpleWorld("test_world")
        val player = server.addPlayer()
        val location = Location(world, -100.5, -10.0, -500.75, -90.0f, -45.0f)
        player.teleport(location)

        previousLocationManager.saveLocation(player)
        val retrievedLocation = previousLocationManager.getSavedLocation(player)

        assertNotNull(retrievedLocation)
        assertEquals(-100.5, retrievedLocation.x)
        assertEquals(-10.0, retrievedLocation.y)
        assertEquals(-500.75, retrievedLocation.z)
        assertEquals(-90.0f, retrievedLocation.yaw)
        assertEquals(-45.0f, retrievedLocation.pitch)
    }

    @Test
    fun `saveLocation handles zero coordinates`() {
        val world = server.addSimpleWorld("test_world")
        val player = server.addPlayer()
        val location = Location(world, 0.0, 0.0, 0.0, 0.0f, 0.0f)
        player.teleport(location)

        previousLocationManager.saveLocation(player)
        val retrievedLocation = previousLocationManager.getSavedLocation(player)

        assertNotNull(retrievedLocation)
        assertEquals(0.0, retrievedLocation.x)
        assertEquals(0.0, retrievedLocation.y)
        assertEquals(0.0, retrievedLocation.z)
        assertEquals(0.0f, retrievedLocation.yaw)
        assertEquals(0.0f, retrievedLocation.pitch)
    }

    @Test
    fun `saveLocation persists across different worlds`() {
        val world1 = server.addSimpleWorld("world_1")
        val world2 = server.addSimpleWorld("world_2")
        val player = server.addPlayer()

        val locationInWorld1 = Location(world1, 50.0, 64.0, 50.0)
        player.teleport(locationInWorld1)
        previousLocationManager.saveLocation(player)

        // Player moves to another world
        player.teleport(Location(world2, 0.0, 64.0, 0.0))

        val retrievedLocation = previousLocationManager.getSavedLocation(player)

        assertNotNull(retrievedLocation)
        assertEquals(world1, retrievedLocation.world)
        assertEquals(50.0, retrievedLocation.x)
    }

    @Test
    fun `multiple players can have different saved locations`() {
        val world = server.addSimpleWorld("test_world")
        val player1 = server.addPlayer("Player1")
        val player2 = server.addPlayer("Player2")

        val location1 = Location(world, 10.0, 64.0, 10.0)
        val location2 = Location(world, 100.0, 70.0, 100.0)

        player1.teleport(location1)
        player2.teleport(location2)

        previousLocationManager.saveLocation(player1)
        previousLocationManager.saveLocation(player2)

        val retrieved1 = previousLocationManager.getSavedLocation(player1)
        val retrieved2 = previousLocationManager.getSavedLocation(player2)

        assertNotNull(retrieved1)
        assertNotNull(retrieved2)
        assertEquals(10.0, retrieved1.x)
        assertEquals(100.0, retrieved2.x)
    }

    @Test
    fun `pdcKey has correct value`() {
        assertEquals("previous_location", previousLocationManager.pdcKey)
    }

    @Test
    fun `saveLocation preserves rotation angles`() {
        val world = server.addSimpleWorld("test_world")
        val player = server.addPlayer()
        val location = Location(world, 10.0, 64.0, 10.0, 180.0f, -90.0f)
        player.teleport(location)

        previousLocationManager.saveLocation(player)
        val retrievedLocation = previousLocationManager.getSavedLocation(player)

        assertNotNull(retrievedLocation)
        assertEquals(180.0f, retrievedLocation.yaw)
        assertEquals(-90.0f, retrievedLocation.pitch)
    }

    @Test
    fun `saveLocation handles very large coordinates`() {
        val world = server.addSimpleWorld("test_world")
        val player = server.addPlayer()
        val location = Location(world, 30000000.0, 256.0, -30000000.0)
        player.teleport(location)

        previousLocationManager.saveLocation(player)
        val retrievedLocation = previousLocationManager.getSavedLocation(player)

        assertNotNull(retrievedLocation)
        assertEquals(30000000.0, retrievedLocation.x)
        assertEquals(-30000000.0, retrievedLocation.z)
    }

    @Test
    fun `saveLocation handles decimal precision`() {
        val world = server.addSimpleWorld("test_world")
        val player = server.addPlayer()
        val location = Location(world, 123.456789, 64.123456, -987.654321, 12.34f, -56.78f)
        player.teleport(location)

        previousLocationManager.saveLocation(player)
        val retrievedLocation = previousLocationManager.getSavedLocation(player)

        assertNotNull(retrievedLocation)
        assertEquals(123.456789, retrievedLocation.x)
        assertEquals(64.123456, retrievedLocation.y)
        assertEquals(-987.654321, retrievedLocation.z)
        assertEquals(12.34f, retrievedLocation.yaw)
        assertEquals(-56.78f, retrievedLocation.pitch)
    }
}
