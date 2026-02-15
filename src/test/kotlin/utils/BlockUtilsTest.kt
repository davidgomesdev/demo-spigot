package utils

import me.davidgomes.demo.Main
import org.bukkit.Location
import org.bukkit.Material
import org.junit.jupiter.api.assertThrows
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.world.WorldMock
import kotlin.test.*

class BlockUtilsTest {
    lateinit var server: ServerMock
    lateinit var world: WorldMock

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        MockBukkit.load(Main::class.java)
        world = server.addSimpleWorld("test_world")
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `hasBlocksBelow returns false when no blocks below`() {
        val location = Location(world, 10.0, 100.0, 20.0)

        assertFalse(hasBlocksBelow(location, 5))
    }

    @Test
    fun `hasBlocksBelow returns true when block directly below`() {
        val location = Location(world, 10.0, 65.0, 20.0)

        // Set a block one level below
        world.getBlockAt(10, 64, 20).type = Material.STONE

        assertTrue(hasBlocksBelow(location, 5))
    }

    @Test
    fun `hasBlocksBelow returns true when block within range`() {
        val location = Location(world, 10.0, 65.0, 20.0)

        // Set a block 3 levels below
        world.getBlockAt(10, 62, 20).type = Material.DIRT

        assertTrue(hasBlocksBelow(location, 5))
    }

    @Test
    fun `hasBlocksBelow returns false when block is beyond range`() {
        val location = Location(world, 10.0, 65.0, 20.0)

        // Set a block 10 levels below (beyond the range of 5)
        world.getBlockAt(10, 55, 20).type = Material.STONE

        assertFalse(hasBlocksBelow(location, 5))
    }

    @Test
    fun `hasBlocksBelow returns true when multiple blocks below`() {
        val location = Location(world, 10.0, 65.0, 20.0)

        // Set multiple blocks below
        world.getBlockAt(10, 64, 20).type = Material.STONE
        world.getBlockAt(10, 63, 20).type = Material.DIRT
        world.getBlockAt(10, 62, 20).type = Material.GRASS_BLOCK

        assertTrue(hasBlocksBelow(location, 5))
    }

    @Test
    fun `hasBlocksBelow returns true with range of 1`() {
        val location = Location(world, 10.0, 65.0, 20.0)

        // Set a block directly below
        world.getBlockAt(10, 64, 20).type = Material.STONE

        assertTrue(hasBlocksBelow(location, 1))
    }

    @Test
    fun `hasBlocksBelow returns false with range of 1 when no block directly below`() {
        val location = Location(world, 10.0, 65.0, 20.0)

        // Set a block 2 levels below (beyond range of 1)
        world.getBlockAt(10, 63, 20).type = Material.STONE

        assertFalse(hasBlocksBelow(location, 1))
    }

    @Test
    fun `hasBlocksBelow throws when location has no world`() {
        val location = Location(null, 10.0, 65.0, 20.0)

        assertThrows<IllegalArgumentException>("Location must contain the world!") {
            hasBlocksBelow(location, 5)
        }
    }

    @Test
    fun `hasBlocksBelow stops checking after finding first block`() {
        val location = Location(world, 10.0, 65.0, 20.0)

        // Set block at level 1 below
        world.getBlockAt(10, 64, 20).type = Material.STONE
        // Also set blocks below that
        world.getBlockAt(10, 63, 20).type = Material.DIRT
        world.getBlockAt(10, 62, 20).type = Material.GRASS_BLOCK

        // Should still return true (it checks and returns early)
        assertTrue(hasBlocksBelow(location, 5))
    }
}
