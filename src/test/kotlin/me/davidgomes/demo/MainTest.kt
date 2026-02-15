package me.davidgomes.demo

import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import me.davidgomes.demo.arena.ArenaEventHandler
import me.davidgomes.demo.heroes.butcher.AnvilDropEventHandler
import me.davidgomes.demo.map.GameMap
import me.davidgomes.demo.map.creation.MapCreationInteractions
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.plugin.PluginManagerMock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

@Suppress("UnstableApiUsage")
class MainTest {
    private lateinit var main: Main
    private lateinit var serverMock: ServerMock
    private lateinit var mockPluginManager: PluginManagerMock

    @BeforeTest
    fun setUp() {
        serverMock = spyk(ServerMock())
        mockPluginManager = spyk(PluginManagerMock(serverMock))

        every { serverMock.pluginManager } returns mockPluginManager

        MockBukkit.mock(serverMock)
        main = MockBukkit.load(Main::class.java)

        // LifecycleManager (for commands) throws, but we can validate other things
        runCatching {
            main.onEnable()
        }
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `onEnable registers GameMap configuration serialization`() {
        val gameMapSerialization = ConfigurationSerialization.getAlias(GameMap::class.java)
        assertNotNull(gameMapSerialization)
    }

    @Test
    fun `onEnable registers Event Handlers`() {
        verify {
            listOf(
                any<MapCreationInteractions>(),
                any<ArenaEventHandler>(),
                any<AnvilDropEventHandler>(),
            ).forEach { eventHandler ->
                mockPluginManager.registerEvents(eventHandler, main)
            }
        }
    }
}
