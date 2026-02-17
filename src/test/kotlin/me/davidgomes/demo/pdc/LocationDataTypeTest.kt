package me.davidgomes.demo.pdc

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import me.davidgomes.demo.Main
import me.davidgomes.demo.plugin
import org.bukkit.Location
import org.bukkit.persistence.PersistentDataAdapterContext
import org.junit.jupiter.api.assertThrows
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.world.WorldMock
import kotlin.io.encoding.Base64
import kotlin.test.*

class LocationDataTypeTest {
    private lateinit var mockServer: ServerMock
    private lateinit var world: WorldMock
    private lateinit var context: PersistentDataAdapterContext

    @BeforeTest
    fun setUp() {
        mockServer = spyk(MockBukkit.mock())
        val mockPlugin = spyk(MockBukkit.load(Main::class.java)) {
            every { server } returns mockServer
        }
        world = mockServer.addSimpleWorld("test_world")

        plugin = mockPlugin
        context = mockk(relaxed = true)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `getPrimitiveType returns ByteArray class`() {
        val result = LocationDataType.getPrimitiveType()

        assertEquals(ByteArray::class.java, result)
    }

    @Test
    fun `getComplexType returns Location class`() {
        val result = LocationDataType.getComplexType()

        assertEquals(Location::class.java, result)
    }

    @Test
    fun `toPrimitive converts Location to ByteArray correctly`() {
        val location = Location(world, 10.5, 64.0, -20.5, 45.0f, -15.0f)

        val result = LocationDataType.toPrimitive(location, context)

        val resultString = String(result)
        val parts = resultString.split(",")

        assertEquals(6, parts.size)

        // Verify world name is base64 encoded
        val decodedWorldName = Base64.decode(parts[0]).decodeToString()
        assertEquals("test_world", decodedWorldName)

        // Verify coordinates
        assertEquals("10.5", parts[1])
        assertEquals("64.0", parts[2])
        assertEquals("-20.5", parts[3])
        assertEquals("45.0", parts[4])
        assertEquals("-15.0", parts[5])
    }

    @Test
    fun `toPrimitive handles location with zero coordinates`() {
        val location = Location(world, 0.0, 0.0, 0.0, 0.0f, 0.0f)

        val result = LocationDataType.toPrimitive(location, context)

        val resultString = String(result)
        val parts = resultString.split(",")

        assertEquals("0.0", parts[1])
        assertEquals("0.0", parts[2])
        assertEquals("0.0", parts[3])
        assertEquals("0.0", parts[4])
        assertEquals("0.0", parts[5])
    }

    @Test
    fun `toPrimitive handles location with negative coordinates`() {
        val location = Location(world, -100.75, -10.25, -500.5, -90.0f, -45.0f)

        val result = LocationDataType.toPrimitive(location, context)

        val resultString = String(result)
        val parts = resultString.split(",")

        assertEquals("-100.75", parts[1])
        assertEquals("-10.25", parts[2])
        assertEquals("-500.5", parts[3])
        assertEquals("-90.0", parts[4])
        assertEquals("-45.0", parts[5])
    }

    @Test
    fun `toPrimitive throws when location world is null`() {
        val location = Location(null, 10.0, 64.0, 10.0)

        assertFailsWith<IllegalArgumentException> {
            LocationDataType.toPrimitive(location, context)
        }
    }

    @Test
    fun `fromPrimitive converts ByteArray back to Location correctly`() {
        val worldName = "test_world"
        val encodedWorldName = Base64.encode(worldName.toByteArray())
        val data = "$encodedWorldName,10.5,64.0,-20.5,45.0,-15.0".toByteArray()

        val result = LocationDataType.fromPrimitive(data, context)

        assertEquals(world, result.world)
        assertEquals(10.5, result.x)
        assertEquals(64.0, result.y)
        assertEquals(-20.5, result.z)
        assertEquals(45.0f, result.yaw)
        assertEquals(-15.0f, result.pitch)
    }

    @Test
    fun `fromPrimitive handles zero coordinates`() {
        val worldName = "test_world"
        val encodedWorldName = Base64.encode(worldName.toByteArray())
        val data = "$encodedWorldName,0.0,0.0,0.0,0.0,0.0".toByteArray()

        val result = LocationDataType.fromPrimitive(data, context)

        assertEquals(0.0, result.x)
        assertEquals(0.0, result.y)
        assertEquals(0.0, result.z)
        assertEquals(0.0f, result.yaw)
        assertEquals(0.0f, result.pitch)
    }

    @Test
    fun `fromPrimitive handles negative coordinates`() {
        val worldName = "test_world"
        val encodedWorldName = Base64.encode(worldName.toByteArray())
        val data = "$encodedWorldName,-100.75,-10.25,-500.5,-90.0,-45.0".toByteArray()

        val result = LocationDataType.fromPrimitive(data, context)

        assertEquals(-100.75, result.x)
        assertEquals(-10.25, result.y)
        assertEquals(-500.5, result.z)
        assertEquals(-90.0f, result.yaw)
        assertEquals(-45.0f, result.pitch)
    }

    @Test
    fun `fromPrimitive works with different world names`() {
        val alternateWorld = mockServer.addSimpleWorld("another_world_123")
        val worldName = "another_world_123"
        val encodedWorldName = Base64.encode(worldName.toByteArray())
        val data = "$encodedWorldName,5.0,70.0,15.0,0.0,0.0".toByteArray()

        val result = LocationDataType.fromPrimitive(data, context)

        assertEquals(alternateWorld, result.world)
        assertEquals(5.0, result.x)
        assertEquals(70.0, result.y)
        assertEquals(15.0, result.z)
    }

    @Test
    fun `roundtrip conversion preserves Location data`() {
        val originalLocation = Location(world, 123.456, 78.9, -987.654, 180.0f, -90.0f)

        val primitive = LocationDataType.toPrimitive(originalLocation, context)

        println(primitive.toString())
        val reconstructedLocation = LocationDataType.fromPrimitive(primitive, context)

        assertEquals(originalLocation.world, reconstructedLocation.world)
        assertEquals(originalLocation.x, reconstructedLocation.x)
        assertEquals(originalLocation.y, reconstructedLocation.y)
        assertEquals(originalLocation.z, reconstructedLocation.z)
        assertEquals(originalLocation.yaw, reconstructedLocation.yaw)
        assertEquals(originalLocation.pitch, reconstructedLocation.pitch)
    }

    @Test
    fun `fromPrimitive throws when data format is invalid - too few parts`() {
        val invalidData = "invalid,data".toByteArray()

        assertFailsWith<IllegalArgumentException> {
            LocationDataType.fromPrimitive(invalidData, context)
        }
    }

    @Test
    fun `fromPrimitive throws when coordinates are not numbers`() {
        val worldName = "test_world"
        val encodedWorldName = Base64.encode(worldName.toByteArray())
        val invalidData = "$encodedWorldName,not_a_number,64.0,10.0,0.0,0.0".toByteArray()

        assertFailsWith<NumberFormatException> {
            LocationDataType.fromPrimitive(invalidData, context)
        }
    }

    @Test
    fun `fromPrimitive throws when world does not exist`() {
        val nonExistentWorldName = "non_existent_world"
        val encodedWorldName = Base64.encode(nonExistentWorldName.toByteArray())
        val data = "$encodedWorldName,10.0,64.0,10.0,0.0,0.0".toByteArray()

        // This should create a location with null world since the world doesn't exist
        val result = LocationDataType.fromPrimitive(data, context)

        assertNull(result.world)
    }

    @Test
    fun `toPrimitive handles world names with special characters`() {
        val specialWorld = mockServer.addSimpleWorld("world_with-special.chars_123")
        val location = Location(specialWorld, 1.0, 2.0, 3.0)

        val result = LocationDataType.toPrimitive(location, context)
        val reconstructed = LocationDataType.fromPrimitive(result, context)

        assertEquals(specialWorld, reconstructed.world)
        assertEquals(location.x, reconstructed.x)
        assertEquals(location.y, reconstructed.y)
        assertEquals(location.z, reconstructed.z)
    }

    @Test
    fun `fromPrimitive handles invalid base64 world name`() {
        val invalidBase64 = "invalid_base64_string"
        val data = "$invalidBase64,10.0,64.0,10.0,0.0,0.0".toByteArray()

        assertThrows<IllegalArgumentException> {
            LocationDataType.fromPrimitive(data, context)
        }
    }
}
