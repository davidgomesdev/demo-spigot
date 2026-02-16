package me.davidgomes.demo.arena

import me.davidgomes.demo.Main
import me.davidgomes.demo.heroes.butcher.ButcherHero
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import java.util.logging.Logger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HeroManagerTest {
    private lateinit var server: ServerMock
    private lateinit var heroManager: HeroManager
    private lateinit var logger: Logger
    private lateinit var plugin: Plugin

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(Main::class.java)
        logger = Logger.getLogger("HeroManagerTest")
        heroManager = HeroManager(plugin, logger)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `setHero stores hero in player persistent data`() {
        val player = server.addPlayer()
        val hero = ButcherHero

        heroManager.setHero(player, hero)

        val storedHero =
            player.persistentDataContainer.get(
                NamespacedKey(plugin, "hero"),
                PersistentDataType.STRING,
            )

        assertEquals(hero.name, storedHero)
    }

    @Test
    fun `getHero retrieves hero from player persistent data`() {
        val player = server.addPlayer()
        val hero = ButcherHero

        heroManager.setHero(player, hero)

        val retrieved = heroManager.getHero(player)

        assertEquals(hero, retrieved)
    }

    @Test
    fun `getHero returns null when player has no hero set`() {
        val player = server.addPlayer()

        val hero = heroManager.getHero(player)

        assertNull(hero)
    }

    @Test
    fun `getHero returns null when hero name does not exist in Hero list`() {
        val player = server.addPlayer()

        player.persistentDataContainer.set(
            NamespacedKey(plugin, "hero"),
            PersistentDataType.STRING,
            "NonExistentHero",
        )

        val hero = heroManager.getHero(player)

        assertNull(hero)
    }
}
