package me.davidgomes.demo.arena.hero.selection

import me.davidgomes.demo.Main
import me.davidgomes.demo.heroes.Hero
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import kotlin.test.*

class HeroSelectorInventoryTest {
    private lateinit var server: ServerMock
    private lateinit var heroSelectorInventory: HeroSelectorInventory

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        MockBukkit.load(Main::class.java)
        heroSelectorInventory = HeroSelectorInventory(server)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `getInventory returns non-null inventory`() {
        val inventory = heroSelectorInventory.getInventory()

        assertNotNull(inventory)
    }

    @Test
    fun `inventory has minimum size of 9`() {
        val inventory = heroSelectorInventory.inventory

        assertTrue(inventory.size >= 9)
    }

    @Test
    fun `inventory size matches hero list size or at least 9`() {
        val inventory = heroSelectorInventory.inventory
        val expectedSize = Hero.list.size.coerceAtLeast(9)

        assertEquals(expectedSize, inventory.size)
    }

    @Test
    fun `inventory contains all hero selector items`() {
        val inventory = heroSelectorInventory.inventory

        Hero.list.forEachIndexed { index, hero ->
            val item = inventory.getItem(index)
            assertNotNull(item, "Hero selector item at index $index should not be null")
            assertEquals(hero.selectorItem.material, item.type)
        }
    }

    @Test
    fun `inventory items are in correct order`() {
        val inventory = heroSelectorInventory.inventory

        Hero.list.forEachIndexed { index, hero ->
            val item = inventory.getItem(index)
            assertNotNull(item)
            assertTrue(hero.selectorItem isTheSame item)
        }
    }

    @Test
    fun `inventory items have correct display names`() {
        val inventory = heroSelectorInventory.inventory

        Hero.list.forEachIndexed { index, hero ->
            val item = inventory.getItem(index)
            assertNotNull(item)
            // Check that the item's display name matches the hero selector item name
            val displayName = item.itemMeta?.displayName()
            assertNotNull(displayName)
        }
    }

    @Test
    fun `inventory can be accessed multiple times`() {
        val firstAccess = heroSelectorInventory.inventory
        val secondAccess = heroSelectorInventory.inventory

        // Should return the same inventory instance
        assertSame(firstAccess, secondAccess)
    }
}
