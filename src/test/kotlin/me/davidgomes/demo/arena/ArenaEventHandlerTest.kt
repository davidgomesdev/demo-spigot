package me.davidgomes.demo.arena

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.Event
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.block.BlockMock
import org.mockbukkit.mockbukkit.entity.ItemMock
import java.util.*
import java.util.logging.Logger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Events are "Unstable API", but it's easier than mocking
@Suppress("UnstableApiUsage")
class ArenaEventHandlerTest {
    private lateinit var server: ServerMock
    private lateinit var arenaManager: ArenaManager
    private lateinit var handler: ArenaEventHandler

    @BeforeEach
    fun setUp() {
        val logger = Logger.getLogger("ArenaEventHandlerTest")

        server = MockBukkit.mock()
        arenaManager = ArenaManager(logger)
        handler = ArenaEventHandler(logger, arenaManager)
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `onPlayerJoin gives player arena join item`() {
        val player = server.addPlayer()
        val event = PlayerJoinEvent(player, Component.text("joined"))

        handler.onPlayerJoin(event)

        val item = player.inventory.getItem(0)

        assertEquals(Material.DIAMOND_SWORD, item?.type)
        assertEquals(Component.text("Join Arena"), item?.itemMeta?.customName())
    }

    @Test
    fun `onPlayerQuit removes player from arena`() {
        val player = server.addPlayer()

        arenaManager.joinArena(player.uniqueId)
        assertTrue(arenaManager.isInArena(player.uniqueId))

        val event = PlayerQuitEvent(player, Component.text("left"), PlayerQuitEvent.QuitReason.DISCONNECTED)

        handler.onPlayerQuit(event)

        assertFalse(arenaManager.isInArena(player.uniqueId))
    }

    @Test
    fun `onPlayerQuit does not fail when the player is not in the arena`() {
        val player = server.addPlayer()

        assertFalse(arenaManager.isInArena(player.uniqueId))

        val event = PlayerQuitEvent(player, Component.text("left"), PlayerQuitEvent.QuitReason.DISCONNECTED)

        handler.onPlayerQuit(event)

        assertFalse(arenaManager.isInArena(player.uniqueId))
    }

    @Test
    fun `onPlayerInteract joins player to arena when using join item on air`() {
        val player = server.addPlayer()
        player.inventory.setItemInMainHand(handler.arenaJoinItem)

        val event = PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, handler.arenaJoinItem, null, BlockFace.SELF)

        handler.onPlayerInteract(event)

        assertTrue(arenaManager.isInArena(player.uniqueId))
        assertEquals(Event.Result.DENY, event.useInteractedBlock())
        assertEquals(Event.Result.DENY, event.useItemInHand())
    }

    @Test
    fun `onPlayerInteract joins player to arena when using join item on a block`() {
        val player = server.addPlayer()
        player.inventory.setItemInMainHand(handler.arenaJoinItem)

        val event =
            PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                handler.arenaJoinItem,
                BlockMock(Material.ACACIA_PLANKS, player.location),
                BlockFace.EAST,
            )

        handler.onPlayerInteract(event)

        assertTrue(arenaManager.isInArena(player.uniqueId))
        assertEquals(Event.Result.DENY, event.useInteractedBlock())
        assertEquals(Event.Result.DENY, event.useItemInHand())
    }

    @Test
    fun `onPlayerInteract does nothing for left click`() {
        val player = server.addPlayer()
        player.inventory.setItemInMainHand(handler.arenaJoinItem)

        val event = PlayerInteractEvent(player, Action.LEFT_CLICK_AIR, handler.arenaJoinItem, null, BlockFace.SELF)

        handler.onPlayerInteract(event)

        assertFalse(arenaManager.isInArena(player.uniqueId))
    }

    @Test
    fun `onPlayerInteract does nothing when player is already in arena`() {
        val player = server.addPlayer()

        arenaManager.joinArena(player.uniqueId)
        player.inventory.setItemInMainHand(handler.arenaJoinItem)

        val event = PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, handler.arenaJoinItem, null, BlockFace.SELF)

        handler.onPlayerInteract(event)

        assertTrue(arenaManager.isInArena(player.uniqueId))
        assertEquals(Event.Result.DENY, event.useInteractedBlock())
        assertEquals(Event.Result.DENY, event.useItemInHand())
    }

    @Test
    fun `onPlayerInteract does nothing for wrong item`() {
        val player = server.addPlayer()
        val wrongItem = ItemStack(Material.STONE)

        player.inventory.setItemInMainHand(wrongItem)

        val event = PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, wrongItem, null, BlockFace.SELF)

        handler.onPlayerInteract(event)

        assertFalse(arenaManager.isInArena(player.uniqueId))
    }

    @Test
    fun `onPlayerInteract does nothing when no item in hand`() {
        val player = server.addPlayer()
        val event = PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, null, null, BlockFace.SELF)

        handler.onPlayerInteract(event)

        assertFalse(arenaManager.isInArena(player.uniqueId))
    }

    @Test
    fun `onPlayerDropItem cancels event when dropping join item`() {
        val player = server.addPlayer()

        player.inventory.setItemInMainHand(handler.arenaJoinItem)

        val event = PlayerDropItemEvent(player, ItemMock(server, UUID.randomUUID(), handler.arenaJoinItem))

        handler.onPlayerDropItem(event)

        assertTrue(event.isCancelled)
        assertEquals(handler.arenaJoinItem, player.inventory.getItem(0))
    }

    @Test
    fun `onPlayerDropItem does not cancel event when dropping a random item`() {
        val player = server.addPlayer()

        // Can't stop the player from flexing on dat drip
        val randomItem = ItemMock(server, UUID.randomUUID(), ItemStack.of(Material.DIAMOND))
        val event = PlayerDropItemEvent(player, randomItem)

        handler.onPlayerDropItem(event)

        assertFalse(event.isCancelled)
    }
}
