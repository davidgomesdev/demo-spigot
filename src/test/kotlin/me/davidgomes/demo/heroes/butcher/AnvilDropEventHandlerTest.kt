package me.davidgomes.demo.heroes.butcher

import io.mockk.*
import me.davidgomes.demo.Main
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.Directional
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.entity.FallingBlockMock
import org.mockbukkit.mockbukkit.entity.PlayerMock
import java.util.*
import java.util.function.Consumer
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

    @BeforeTest
    fun setUp() {
        logger = Logger.getLogger("AnvilDropEventHandlerTest")

        server = MockBukkit.mock()
        plugin = MockBukkit.load(Main::class.java)

        handler = AnvilDropEventHandler(plugin, logger)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `onPlayerRightClickAnvil cancels event when player right clicks with anvil`() {
        val player = mockk<Player>(relaxed = true)
        val world = mockk<World>(relaxed = true)
        val block = mockk<Block>(relaxed = true)
        val blockData = mockk<BlockData>(relaxed = true)
        val anvilBlockData = mockk<Directional>(relaxed = true)
        val fallingAnvil = mockk<FallingBlock>(relaxed = true)
        val persistentDataContainer = mockk<PersistentDataContainer>(relaxed = true)
        val item = mockk<ItemStack>(relaxed = true)
        val event = mockk<PlayerInteractEvent>(relaxed = true)

        every { event.item } returns item
        every { item.type } returns Material.ANVIL
        every { event.action } returns Action.RIGHT_CLICK_AIR
        every { event.player } returns player
        every { player.name } returns "TestPlayer"
        every { player.facing } returns BlockFace.NORTH
        every { player.uniqueId } returns UUID.randomUUID()

        // Block in line of sight
        every { block.isEmpty } returns false
        every { block.location } returns Location(world, 10.0, 65.0, 10.0)
        every { block.blockData } returns blockData
        every { blockData.material } returns Material.STONE
        every { player.getLineOfSight(null, AnvilAbilityAttributes.MAX_CAST_DISTANCE) } returns listOf(block)

        // World setup for spawning
        every { world.getBlockAt(any<Int>(), any<Int>(), any<Int>()) } returns block
        every { block.blockData.material } returns Material.AIR

        every { anvilBlockData.facing = any() } just Runs

        every { world.spawn(any<Location>(), FallingBlock::class.java, any<Consumer<FallingBlock>>()) } answers {
            val consumer = thirdArg<Consumer<FallingBlock>>()
            consumer.accept(fallingAnvil)
            fallingAnvil
        }
        every { fallingAnvil.persistentDataContainer } returns persistentDataContainer

        handler.onPlayerRightClickAnvil(event)

        verify { event.isCancelled = true }
    }

    @Test
    fun `onPlayerRightClickAnvil does nothing when item is not anvil`() {
        val item = mockk<ItemStack>(relaxed = true)
        val event = mockk<PlayerInteractEvent>(relaxed = true)

        every { event.item } returns item
        every { item.type } returns Material.DIAMOND_SWORD

        handler.onPlayerRightClickAnvil(event)

        verify(exactly = 0) { event.isCancelled = any() }
    }

    @Test
    fun `onPlayerRightClickAnvil does nothing when action is not right click`() {
        val item = mockk<ItemStack>(relaxed = true)
        val event = mockk<PlayerInteractEvent>(relaxed = true)

        every { event.item } returns item
        every { item.type } returns Material.ANVIL
        every { event.action } returns Action.LEFT_CLICK_AIR

        handler.onPlayerRightClickAnvil(event)

        verify(exactly = 0) { event.isCancelled = any() }
    }

    @Test
    fun `onAnvilHit damages nearby entities`() {
        // TODO: see if every mock is needed after having it work
        val serverSpy = spyk(server)

        val anvilDataMock = mockk<Directional>(relaxed = true)

        every { anvilDataMock.facing = any() } just Runs

        every { serverSpy.createBlockData(Material.ANVIL) } returns anvilDataMock

        val sender = spyk(server.addPlayer("sender"))

        every { sender.facing } returns BlockFace.EAST

        val world = spyk(sender.world)
        val hitLocation = Location(world, 10.0, 65.0, 10.0)

        val target = spyk(PlayerMock(server, "target")).apply {
            location = hitLocation
        }

        // Fixes Stackoverflow of LivingEntityMock.isDead
        every { target.isDead } returns false

        server.addPlayer(target)

        logger.info("Sender UUID: ${sender.uniqueId}, Target UUID: ${target.uniqueId}")

        val block = spyk(world.spawn(hitLocation, FallingBlockMock::class.java))
        //noinspection UnstableApiUsage
        val event = spyk(EntityChangeBlockEvent(block, block.location.block, block.blockData))

        handler.setAnvilProperties(block, sender)

        handler.onAnvilHit(event)
        logger.info("hi")

        verify {
            target.damage(
                AnvilAbilityAttributes.Landing.DROP_DAMAGE,
                match<Entity> { it.name == "sender" }
            )
        }
        verify { world.playEffect(hitLocation, Effect.ANVIL_LAND, null) }
        verify { world.playSound(hitLocation, Sound.BLOCK_ANVIL_HIT, any(), any()) }
        verify { event.isCancelled = true }
    }

    @Test
    fun `onAnvilHit does nothing when entity is not falling block`() {
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
    fun `onAnvilHit does nothing when block is not air`() {
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
    fun `onAnvilHit does nothing when falling block is not anvil`() {
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
    fun `onAnvilHit does nothing when sender not found`() {
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
