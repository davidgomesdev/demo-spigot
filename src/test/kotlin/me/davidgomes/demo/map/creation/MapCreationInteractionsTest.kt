package me.davidgomes.demo.map.creation

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import me.davidgomes.demo.Main
import me.davidgomes.demo.arena.model.Team
import me.davidgomes.demo.createPlayer
import me.davidgomes.demo.map.MapManager
import me.davidgomes.demo.messages.CANNOT_FINISH_YET_MESSAGE
import me.davidgomes.demo.messages.NOT_IN_SESSION_MESSAGE
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.inventory.ItemStackMock
import java.util.logging.Logger
import kotlin.test.*

@Suppress("UnstableApiUsage")
class MapCreationInteractionsTest {
    private lateinit var server: ServerMock
    private lateinit var mapManager: MapManager
    private lateinit var manager: MapCreationManager
    private lateinit var interactions: MapCreationInteractions

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        MockBukkit.load(Main::class.java)
        mapManager = mockk(relaxed = true)
        val logger = Logger.getLogger("MapCreationInteractionsTest")
        manager = MapCreationManager(Logger.getLogger("Test"), mapManager, mockk(relaxed = true))
        interactions = MapCreationInteractions(logger, manager)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `onPlayerSetSpawn does nothing when action is not right click`() {
        val player = server.addPlayer()
        val event = spyk(PlayerInteractEvent(player, Action.LEFT_CLICK_AIR, null, null, BlockFace.NORTH))

        interactions.onPlayerSetSpawn(event)

        verify(exactly = 0) { event.isCancelled = any() }
    }

    @Test
    fun `onPlayerSetSpawn alerts when player is trying to set spawn but is not in session`() {
        val player = spyk(server.addPlayer())
        val item = MapCreationItems.spawnPickers[Team.Yellow]!!
        val event = spyk(PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, item, null, BlockFace.NORTH))

        interactions.onPlayerSetSpawn(event)

        verify { player.sendMessage(NOT_IN_SESSION_MESSAGE) }
        verify(exactly = 0) { event.isCancelled = any() }
    }

    @Test
    fun `onPlayerSetSpawn does nothing when item is not a spawn picker`() {
        val player = server.addPlayer()
        every { mapManager existsMapWithName any() } returns false
        manager.createSession(player, "test_map")

        val nonSpawnPickerItem = ItemStackMock(Material.DIAMOND_SWORD)
        val event = spyk(PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, nonSpawnPickerItem, null, BlockFace.NORTH))

        interactions.onPlayerSetSpawn(event)

        verify(exactly = 0) { event.isCancelled = any() }
    }

    @Test
    fun `onPlayerSetSpawn sets spawn when player right clicks with spawn picker`() {
        val player = spyk(server.addPlayer())
        every { mapManager existsMapWithName any() } returns false
        manager.createSession(player, "test_map")

        val item = MapCreationItems.spawnPickers[Team.Yellow]!!
        val event = spyk(PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, item, null, BlockFace.NORTH))

        interactions.onPlayerSetSpawn(event)

        verify { event.isCancelled = true }
        val session = manager.getSession(player)!!
        assertNotNull(session.spawns[Team.Yellow])
        assertEquals(player.location, session.spawns[Team.Yellow])
    }

    @Test
    fun `onPlayerFinishCreation does nothing when action is not right click`() {
        val player = server.addPlayer()
        val event = spyk(PlayerInteractEvent(player, Action.LEFT_CLICK_AIR, null, null, BlockFace.NORTH))

        interactions.onPlayerFinishCreation(event)

        verify(exactly = 0) { event.isCancelled = any() }
    }

    @Test
    fun `onPlayerFinishCreation alerts player when not in session`() {
        val player = spyk(server.addPlayer())
        val item = MapCreationItems.finishCreation
        val event = spyk(PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, item, null, BlockFace.NORTH))

        interactions.onPlayerFinishCreation(event)

        verify { player.sendMessage(NOT_IN_SESSION_MESSAGE) }
        verify(exactly = 0) { event.isCancelled = any() }
    }

    @Test
    fun `onPlayerFinishCreation does nothing when item is not finish creation`() {
        val player = spyk(server.addPlayer())
        every { mapManager existsMapWithName any() } returns false
        manager.createSession(player, "test_map")

        val wrongItem = ItemStackMock(Material.DIAMOND_SWORD)
        val event = PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, wrongItem, null, BlockFace.NORTH)

        interactions.onPlayerFinishCreation(event)

        // Player should still be in session
        assertTrue(manager isInSession player)
    }

    @Test
    fun `onPlayerFinishCreation alerts player when not all spawns are set`() {
        val world = server.addSimpleWorld("test_world")
        val player = spyk(server.addPlayer())
        every { mapManager existsMapWithName any() } returns false
        manager.createSession(player, "test_map")

        val session = manager.getSession(player)!!
        // Only set one spawn, not both
        session.setSpawn(Team.Yellow, Location(world, 0.0, 0.0, 0.0))

        val item = MapCreationItems.finishCreation
        val event = spyk(PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, item, null, BlockFace.NORTH))

        interactions.onPlayerFinishCreation(event)

        verify { player.sendMessage(CANNOT_FINISH_YET_MESSAGE) }
        // Player should still be in session
        assertFalse(manager isNotInSession player)
    }

    @Test
    fun `onPlayerQuit removes player session`() {
        val player = createPlayer(server)
        every { mapManager existsMapWithName any() } returns false
        manager.createSession(player, "test_map")

        val event = PlayerQuitEvent(player, Component.empty(), PlayerQuitEvent.QuitReason.DISCONNECTED)

        interactions.onPlayerQuit(event)

        assertTrue(manager isNotInSession player)
    }

    @Test
    fun `onPlayerDeath removes player session`() {
        val player = createPlayer(server)
        every { mapManager existsMapWithName any() } returns false
        manager.createSession(player, "test_map")

        val event =
            PlayerDeathEvent(
                player,
                DamageSource.builder(DamageType.PLAYER_ATTACK).build(),
                emptyList(),
                0,
                Component.empty(),
                false,
            )

        interactions.onPlayerDeath(event)

        assertTrue(manager isNotInSession player)
    }

    @Test
    fun `onPlayerDropItem cancels when session is complete and item is finish creation`() {
        val world = server.addSimpleWorld("test_world")
        val player = server.addPlayer()
        every { mapManager existsMapWithName any() } returns false
        manager.createSession(player, "test_map")

        val session = manager.getSession(player)!!
        session.setSpawn(Team.Yellow, Location(world, 0.0, 0.0, 0.0))
        session.setSpawn(Team.Blue, Location(world, 10.0, 0.0, 0.0))

        val item = MapCreationItems.finishCreation
        val event = spyk(PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, item, null, BlockFace.NORTH))

        interactions.onPlayerDropItem(event)

        verify { event.isCancelled = true }
    }
}
