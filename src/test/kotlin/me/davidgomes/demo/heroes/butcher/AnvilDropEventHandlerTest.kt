package me.davidgomes.demo.heroes.butcher

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import me.davidgomes.demo.Main
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.Nested
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.entity.FallingBlockMock
import org.mockbukkit.mockbukkit.entity.PlayerMock
import org.mockbukkit.mockbukkit.inventory.ItemStackMock
import org.mockbukkit.mockbukkit.world.WorldMock
import java.util.logging.Logger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

// Events are "Unstable API", but it's easier than mocking
@Suppress("UnstableApiUsage")
class AnvilDropEventHandlerTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: Plugin
    private lateinit var logger: Logger
    private lateinit var handler: AnvilDropEventHandler
    private lateinit var world: WorldMock

    @BeforeTest
    fun setUp() {
        logger = Logger.getLogger("AnvilDropEventHandlerTest")

        server = MockBukkit.mock()
        world = spyk(server.addSimpleWorld("world"))
        plugin = MockBukkit.load(Main::class.java)

        handler = AnvilDropEventHandler(plugin, logger)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Nested
    inner class OnAnvilTrigger {
        @Test
        fun `does nothing when item is not anvil`() {
            val item = ItemStackMock(Material.DIAMOND_SWORD)
            val player = server.addPlayer()
            val event = spyk(PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, item, null, BlockFace.NORTH))

            handler.onPlayerRightClickAnvil(event)

            verify(exactly = 0) { event.isCancelled = any() }
        }

        @Test
        fun `does nothing when action is not right click`() {
            val item = ItemStackMock(Material.ANVIL)
            val player = server.addPlayer()
            val event = spyk(PlayerInteractEvent(player, Action.LEFT_CLICK_AIR, item, null, BlockFace.NORTH))

            handler.onPlayerRightClickAnvil(event)

            verify(exactly = 0) { event.isCancelled = any() }
        }
    }

    @Nested
    inner class OnAnvilHit {
        @Test
        fun `damages nearby entities`() {
            val sender = spyk(server.addPlayer("sender"))

            // needed because this is not implemented in MockBukkit
            every { sender.facing } returns BlockFace.EAST

            val world = spyk(sender.world)
            val hitLocation = Location(world, 10.0, 65.0, 10.0)

            val target =
                spyk(PlayerMock(server, "target")).apply {
                    location = hitLocation
                }

            // Fixes Stackoverflow on LivingEntityMock.isDead 🤷
            every { target.isDead } returns false

            server.addPlayer(target)

            val block =
                spyk(world.spawn(hitLocation, FallingBlockMock::class.java))
            val event = spyk(EntityChangeBlockEvent(block, block.location.block, block.blockData))

            handler.setAnvilProperties(block, sender)
            handler.onAnvilHit(event)

            verify {
                target.damage(
                    AnvilAbilityAttributes.Landing.DROP_DAMAGE,
                    match<Entity> { it.name == "sender" },
                )
            }
            verify { world.playEffect(hitLocation, Effect.ANVIL_LAND, null) }
            verify { world.playSound(hitLocation, Sound.BLOCK_ANVIL_HIT, any(), any()) }
            verify { event.isCancelled = true }
        }

        @Test
        fun `does nothing when entity is not falling block`() {
            val entity = mockk<Player>(relaxed = true)
            val block = mockk<Block>(relaxed = true)
            val event = mockk<EntityChangeBlockEvent>(relaxed = true)

            every { event.entity } returns entity
            every { entity.type } returns EntityType.PLAYER
            every { event.block } returns block
            every { block.type } returns Material.AIR

            handler.onAnvilHit(event)

            verify(exactly = 0) { event.isCancelled = any() }
        }

        @Test
        fun `does nothing when block is not air`() {
            val fallingBlock = mockk<FallingBlock>(relaxed = true)
            val block = mockk<Block>(relaxed = true)
            val event = mockk<EntityChangeBlockEvent>(relaxed = true)

            every { event.entity } returns fallingBlock
            every { fallingBlock.type } returns EntityType.FALLING_BLOCK
            every { event.block } returns block
            every { block.type } returns Material.STONE

            handler.onAnvilHit(event)

            verify(exactly = 0) { event.isCancelled = any() }
        }

        @Test
        fun `does nothing when falling block is not anvil`() {
            val fallingBlock = mockk<FallingBlock>(relaxed = true)
            val block = mockk<Block>(relaxed = true)
            val blockData = mockk<BlockData>(relaxed = true)
            val event = mockk<EntityChangeBlockEvent>(relaxed = true)

            every { event.entity } returns fallingBlock
            every { fallingBlock.type } returns EntityType.FALLING_BLOCK
            every { event.block } returns block
            every { block.type } returns Material.AIR
            every { fallingBlock.blockData } returns blockData
            every { blockData.material } returns Material.SAND

            handler.onAnvilHit(event)

            verify(exactly = 0) { event.isCancelled = any() }
        }

        @Test
        fun `does nothing when sender not found`() {
            val world = mockk<World>(relaxed = true)
            val fallingBlock = mockk<FallingBlock>(relaxed = true)
            val block = mockk<Block>(relaxed = true)
            val blockData = mockk<BlockData>(relaxed = true)
            val persistentDataContainer = mockk<PersistentDataContainer>(relaxed = true)
            val event = mockk<EntityChangeBlockEvent>(relaxed = true)
            val hitLocation = Location(world, 10.0, 65.0, 10.0)

            every { event.entity } returns fallingBlock
            every { event.block } returns block
            every { block.type } returns Material.AIR
            every { fallingBlock.type } returns EntityType.FALLING_BLOCK
            every { fallingBlock.blockData } returns blockData
            every { blockData.material } returns Material.ANVIL
            every { fallingBlock.location } returns hitLocation
            every { fallingBlock.persistentDataContainer } returns persistentDataContainer

            every {
                persistentDataContainer.get(any<NamespacedKey>(), PersistentDataType.STRING)
            } returns null

            handler.onAnvilHit(event)

            verify(exactly = 0) { event.isCancelled = any() }
        }
    }
}
