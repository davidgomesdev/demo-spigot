package me.davidgomes.demo.map.creation

import com.mojang.brigadier.tree.ArgumentCommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import io.mockk.mockk
import io.mockk.spyk
import me.davidgomes.demo.Main
import me.davidgomes.demo.map.MapManager
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import java.util.logging.Logger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MapCreationCommandsTest {
    private lateinit var server: ServerMock
    private lateinit var logger: Logger
    private lateinit var mapManager: MapManager
    private lateinit var manager: MapCreationManager
    private lateinit var commands: MapCreationCommands

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        MockBukkit.load(Main::class.java)
        logger = Logger.getLogger("MapCreationCommandsTest")
        mapManager = mockk(relaxed = true)
        manager = spyk(MapCreationManager(logger, mapManager))
        commands = MapCreationCommands(logger, manager)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `createMap command has correct structure`() {
        val commandNode = commands.createMap.children

        val action = commandNode.elementAt(0) as LiteralCommandNode<*>
        val arg = commandNode.elementAt(1) as ArgumentCommandNode<*, *>

        assertEquals("mg", commands.createMap.name)
        assertEquals("create_map", action.name)
        assertEquals("map_name", arg.name)
        assertEquals(2, commandNode.size)
    }
}

