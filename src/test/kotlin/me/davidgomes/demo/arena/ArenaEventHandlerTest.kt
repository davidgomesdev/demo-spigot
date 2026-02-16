package me.davidgomes.demo.arena

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import me.davidgomes.demo.Main
import me.davidgomes.demo.map.GameMap
import me.davidgomes.demo.map.MapManager
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.event.Event
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.*
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.block.BlockMock
import org.mockbukkit.mockbukkit.entity.ItemMock
import java.util.*
import java.util.logging.Logger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

// Events are "Unstable API", but it's easier than mocking
@Suppress("UnstableApiUsage")
class ArenaEventHandlerTest {
    private lateinit var server: ServerMock
    private lateinit var arenaManager: ArenaManager
    private lateinit var handler: ArenaEventHandler
    private lateinit var heroSelectorInventory: HeroSelectorInventory
    private lateinit var mapManager: MapManager

    @BeforeEach
    fun setUp() {
        val logger = Logger.getLogger("ArenaEventHandlerTest")

        server = MockBukkit.mock()

        val plugin = MockBukkit.load(Main::class.java)

        mapManager = mockk()

        val world = server.addSimpleWorld("world")

        every { mapManager.getAllMaps() } returns listOf(
            GameMap(
                "TestMap",
                mapOf(
                    Team.Blue to Location(world, 0.0, 64.0, 0.0),
                    Team.Yellow to Location(world, 5.0, 64.0, 5.0),
                )
            )
        )

        arenaManager = ArenaManager(
            plugin, logger, HeroManager(plugin, logger), mapManager
        )
        heroSelectorInventory = spyk(HeroSelectorInventory(server))
        handler = ArenaEventHandler(logger, arenaManager, heroSelectorInventory)
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

        arenaManager.joinArena(player)
        assertTrue(arenaManager.isInArena(player))

        val event = PlayerQuitEvent(player, Component.text("left"), PlayerQuitEvent.QuitReason.DISCONNECTED)

        handler.onPlayerQuit(event)

        assertFalse(arenaManager.isInArena(player))
    }

    @Test
    fun `onPlayerQuit does not fail when the player is not in the arena`() {
        val player = server.addPlayer()

        assertFalse(arenaManager.isInArena(player))

        val event = PlayerQuitEvent(player, Component.text("left"), PlayerQuitEvent.QuitReason.DISCONNECTED)

        handler.onPlayerQuit(event)

        assertFalse(arenaManager.isInArena(player))
    }

    @Test
    fun `onPlayerInteract joins player to arena when using join item on air`() {
        val player = server.addPlayer()
        player.inventory.setItemInMainHand(ArenaItems.join)

        val event = PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, ArenaItems.join, null, BlockFace.SELF)

        handler.onPlayerInteractWithArenaJoin(event)

        assertTrue(arenaManager.isInArena(player))
        assertEquals(Event.Result.DENY, event.useInteractedBlock())
        assertEquals(Event.Result.DENY, event.useItemInHand())
    }

    @Test
    fun `onPlayerInteract joins player to arena when using join item on a block`() {
        val player = server.addPlayer()
        player.inventory.setItemInMainHand(ArenaItems.join)

        val event =
            PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                ArenaItems.join,
                BlockMock(Material.ACACIA_PLANKS, player.location),
                BlockFace.EAST,
            )

        handler.onPlayerInteractWithArenaJoin(event)

        assertTrue(arenaManager.isInArena(player))
        assertEquals(Event.Result.DENY, event.useInteractedBlock())
        assertEquals(Event.Result.DENY, event.useItemInHand())
    }

    @Test
    fun `onPlayerInteract does nothing for left click`() {
        val player = server.addPlayer()
        player.inventory.setItemInMainHand(ArenaItems.join)

        val event = PlayerInteractEvent(player, Action.LEFT_CLICK_AIR, ArenaItems.join, null, BlockFace.SELF)

        handler.onPlayerInteractWithArenaJoin(event)

        assertFalse(arenaManager.isInArena(player))
    }

    @Test
    fun `onPlayerInteract does nothing when player is already in arena`() {
        val player = server.addPlayer()

        arenaManager.joinArena(player)
        player.inventory.setItemInMainHand(ArenaItems.join)

        val event = PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, ArenaItems.join, null, BlockFace.SELF)

        handler.onPlayerInteractWithArenaJoin(event)

        assertTrue(arenaManager.isInArena(player))
        assertEquals(Event.Result.DENY, event.useInteractedBlock())
        assertEquals(Event.Result.DENY, event.useItemInHand())
    }

    @Test
    fun `onPlayerInteract does nothing for wrong item`() {
        val player = server.addPlayer()
        val wrongItem = ItemStack(Material.STONE)

        player.inventory.setItemInMainHand(wrongItem)

        val event = PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, wrongItem, null, BlockFace.SELF)

        handler.onPlayerInteractWithArenaJoin(event)

        assertFalse(arenaManager.isInArena(player))
    }

    @Test
    fun `onPlayerInteract does nothing when no item in hand`() {
        val player = server.addPlayer()
        val event = PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, null, null, BlockFace.SELF)

        handler.onPlayerInteractWithArenaJoin(event)

        assertFalse(arenaManager.isInArena(player))
    }

    @Test
    fun `onPlayerDropItem cancels event when dropping join item`() {
        val player = server.addPlayer()

        player.inventory.setItemInMainHand(ArenaItems.join)

        val event = PlayerDropItemEvent(player, ItemMock(server, UUID.randomUUID(), ArenaItems.join))

        handler.onPlayerDropItem(event)

        assertTrue(event.isCancelled)
        assertEquals(ArenaItems.join, player.inventory.getItem(0))
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

    @Nested
    inner class InsideArena {
        @Test
        fun `onPlayerInteractWithArenaStart does nothing for left click`() {
            val player = server.addPlayer()
            arenaManager.joinArena(player)
            player.inventory.setItemInMainHand(ArenaItems.start)

            val event = PlayerInteractEvent(player, Action.LEFT_CLICK_AIR, ArenaItems.start, null, BlockFace.SELF)

            handler.onPlayerInteractWithArenaStart(event)

            assertFalse(arenaManager.isMatchOnGoing())
        }

        @Test
        fun `onPlayerInteractWithArenaStart does nothing for wrong item`() {
            val player = server.addPlayer()

            arenaManager.joinArena(player)
            arenaManager.joinArena(server.addPlayer())

            val wrongItem = ItemStack(Material.STONE)
            player.inventory.setItemInMainHand(wrongItem)

            val event = PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, wrongItem, null, BlockFace.SELF)

            handler.onPlayerInteractWithArenaStart(event)

            assertFalse(arenaManager.isMatchOnGoing())
        }

        @Test
        fun `onPlayerInteractWithArenaStart does nothing when player is not in arena`() {
            val player = server.addPlayer()

            player.inventory.setItemInMainHand(ArenaItems.start)

            val event = PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, ArenaItems.start, null, BlockFace.SELF)

            handler.onPlayerInteractWithArenaStart(event)

            assertFalse(arenaManager.isMatchOnGoing())
            assertEquals(Event.Result.DENY, event.useInteractedBlock())
            assertEquals(Event.Result.DENY, event.useItemInHand())
        }

        @Test
        fun `onPlayerInteractWithArenaStart does nothing when not enough players`() {
            val player = server.addPlayer()

            arenaManager.joinArena(player)

            player.inventory.setItemInMainHand(ArenaItems.start)

            val event = PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, ArenaItems.start, null, BlockFace.SELF)

            handler.onPlayerInteractWithArenaStart(event)

            assertFalse(arenaManager.isMatchOnGoing())
            assertEquals(Event.Result.DENY, event.useInteractedBlock())
            assertEquals(Event.Result.DENY, event.useItemInHand())
        }

        @Test
        fun `onPlayerInteractWithArenaStart starts arena when conditions are met`() {
            val player1 = server.addPlayer()
            val player2 = server.addPlayer()
            arenaManager.joinArena(player1)
            arenaManager.joinArena(player2)
            player1.inventory.setItemInMainHand(ArenaItems.start)

            val event = PlayerInteractEvent(player1, Action.RIGHT_CLICK_AIR, ArenaItems.start, null, BlockFace.SELF)

            handler.onPlayerInteractWithArenaStart(event)

            assertTrue(arenaManager.isMatchOnGoing())
            assertEquals(Event.Result.DENY, event.useInteractedBlock())
            assertEquals(Event.Result.DENY, event.useItemInHand())
        }

        @Test
        fun `onPlayerDeath does nothing when player is not in arena`() {
            val player = server.addPlayer()
            val killer = server.addPlayer()

            val event = PlayerDeathEvent(
                player,
                DamageSource.builder(DamageType.PLAYER_ATTACK).withCausingEntity(killer).build(),
                emptyList<ItemStack>(),
                0,
                Component.text(""),
                false
            )

            assertDoesNotThrow {
                handler.onPlayerDeath(event)
            }
            assertFalse(arenaManager.isMatchOnGoing())
        }

        @Test
        fun `onPlayerDeath does nothing when match is not ongoing`() {
            val player = server.addPlayer()
            val killer = server.addPlayer()

            arenaManager.joinArena(player)
            arenaManager.joinArena(killer)

            assertFalse(arenaManager.isMatchOnGoing())

            val event = PlayerDeathEvent(
                player,
                DamageSource.builder(DamageType.PLAYER_ATTACK).withCausingEntity(killer).build(),
                emptyList<ItemStack>(),
                0,
                Component.text(""),
                false
            )

            assertDoesNotThrow {
                handler.onPlayerDeath(event)
            }
            assertFalse(arenaManager.isMatchOnGoing())
        }

        @Test
        fun `onPlayerDeath records kill when player killed by another player`() {
            val victim = server.addPlayer()
            val killer = server.addPlayer()

            arenaManager.joinArena(victim)
            arenaManager.joinArena(killer)
            arenaManager.startArena(GameType.TeamDeathMatch)

            val killerTeam = arenaManager.getTeam(killer)!!
            val stateBefore = arenaManager.getState() as ArenaState.OnGoingTeamDeathMatch

            stateBefore.scoreboard.values.forEach { assertEquals(0, it.get()) }

            val event = PlayerDeathEvent(
                victim,
                DamageSource.builder(DamageType.PLAYER_ATTACK).withCausingEntity(killer).build(),
                emptyList<ItemStack>(),
                0,
                Component.text(""),
                false
            )

            handler.onPlayerDeath(event)

            val stateAfter = assertIs<ArenaState.OnGoingTeamDeathMatch>(arenaManager.getState())

            assertEquals(1, stateAfter.scoreboard[killerTeam]?.get())
        }

        @Test
        fun `onPlayerDeath does nothing when damage source has no causing entity`() {
            val victim = server.addPlayer()
            arenaManager.joinArena(victim)
            arenaManager.joinArena(server.addPlayer())
            arenaManager.startArena(GameType.TeamDeathMatch)

            val event = PlayerDeathEvent(
                victim, DamageSource.builder(DamageType.FALL).build(),
                emptyList<ItemStack>(),
                0,
                Component.text(""),
                false
            )

            handler.onPlayerDeath(event)

            val stateAfter = arenaManager.getState() as ArenaState.OnGoingTeamDeathMatch

            assertEquals(0, stateAfter.scoreboard[Team.Yellow]?.get())
            assertEquals(0, stateAfter.scoreboard[Team.Blue]?.get())
        }
    }
}
