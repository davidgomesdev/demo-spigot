package me.davidgomes.demo.map.creation

import me.davidgomes.demo.Main
import me.davidgomes.demo.arena.model.Team
import org.bukkit.Material
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.inventory.ItemStackMock
import kotlin.test.*

class MapCreationItemsTest {
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

    @Test
    fun `finishCreation item has correct material`() {
        assertEquals(Material.EMERALD_BLOCK, MapCreationItems.finishCreation.material)
    }

    @Test
    fun `finishCreation item has correct name`() {
        assertEquals("Finish Map Creation", MapCreationItems.finishCreation.name)
    }

    @Test
    fun `spawnPickers contains all teams`() {
        assertEquals(Team.entries.size, MapCreationItems.spawnPickers.size)
        Team.entries.forEach { team ->
            assertTrue(MapCreationItems.spawnPickers.containsKey(team))
        }
    }

    @Test
    fun `spawnPickers have correct materials for each team`() {
        Team.entries.forEach { team ->
            val picker = MapCreationItems.spawnPickers[team]!!
            assertEquals(team.spawnItemMaterial, picker.material)
        }
    }

    @Test
    fun `spawnPickers have correct names for each team`() {
        Team.entries.forEach { team ->
            val picker = MapCreationItems.spawnPickers[team]!!
            assertEquals("Spawn Picker for Team ${team.name}", picker.name)
        }
    }

    @Test
    fun `getTeamFromSpawnPicker returns correct team`() {
        Team.entries.forEach { team ->
            val picker = MapCreationItems.spawnPickers[team]!!
            val foundTeam = MapCreationItems.getTeamFromSpawnPicker(picker)
            assertEquals(team, foundTeam)
        }
    }

    @Test
    fun `getTeamFromSpawnPicker returns null for finish creation item`() {
        val randomItem = ItemStackMock(Material.EMERALD_BLOCK)
        val result = MapCreationItems.getTeamFromSpawnPicker(randomItem)
        assertNull(result)
    }

    @Test
    fun `getTeamFromSpawnPicker returns null for item with random material`() {
        val wrongMaterialItem = ItemStackMock(Material.STONE)
        val result = MapCreationItems.getTeamFromSpawnPicker(wrongMaterialItem)
        assertNull(result)
    }
}
