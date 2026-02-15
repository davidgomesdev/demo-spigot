package me.davidgomes.demo.heroes.butcher

import me.davidgomes.demo.items.InteractableItem
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Directional
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import utils.hasBlocksBelow
import utils.isNotRightClick
import java.util.*
import java.util.logging.Logger
import kotlin.math.roundToInt

private const val SENDER_TAG = "sender"

// TODO: add tests
class AnvilDropEventHandler(
    val plugin: Plugin,
    val logger: Logger,
) : Listener {
    val anvilDropItem =
        InteractableItem(
            material = Material.ANVIL,
            name = "Anvil Drop",
        )

    @EventHandler
    fun onPlayerRightClickAnvil(evt: PlayerInteractEvent) {
        if (evt.isNotRightClick()) return
        if (anvilDropItem isNotTheSame evt.item) return

        logger.info("Player ${evt.player.name} casted the anvil ability")

        // Stop interaction when it's an ability cast (e.g. to prevent placing the anvil or right-clicking on a chest)
        evt.isCancelled = true

        with(AnvilAbilityAttributes) {
            val blocksInSight = evt.player.getLineOfSight(null, MAX_CAST_DISTANCE)
            val blockInSight = blocksInSight.firstOrNull { !it.isEmpty } ?: return

            val spawnLocation = blockInSight.location.add(0.0, FALL_HEIGHT, 0.0)

            // probably when the player is dead
            if (spawnLocation.world == null) {
                return
            }

            if (hasBlocksBelow(spawnLocation, FALL_HEIGHT.roundToInt())) return

            spawnFallingAnvil(spawnLocation, evt.player)
        }
    }

    @EventHandler
    fun onAnvilHit(evt: EntityChangeBlockEvent) {
        if (evt.entity.type != EntityType.FALLING_BLOCK || evt.block.type != Material.AIR) return

        val fallingBlock = evt.entity as FallingBlock

        if (fallingBlock.blockData.material != anvilDropItem.material) return

        val sender = getSenderOf(fallingBlock) ?: return

        val hitLocation = fallingBlock.location
        val world = hitLocation.world

        if (world == null) {
            logger.warning("Couldn't find world on anvil's location!")
            return
        }

        with(AnvilAbilityAttributes.Landing) {
            with(AOE) {
                world
                    .getNearbyEntities(hitLocation, x, y, z)
                    .filterIsInstance<Damageable>()
                    .forEach {
                        logger.info("Damaging '${it.name}'")
                        it.damage(DROP_DAMAGE, sender)
                    }
            }
            world.playEffect(hitLocation, EFFECT, null)
            world.playSound(hitLocation, SOUND, VOLUME, pitch)
        }

        evt.isCancelled = true
    }

    fun spawnFallingAnvil(
        spawnLocation: Location,
        sender: Player,
    ): FallingBlock {
        val world = spawnLocation.world

        return world.spawn(spawnLocation, FallingBlock::class.java) { anvil ->
            setAnvilProperties(anvil, sender)
        }
    }

    fun setAnvilProperties(
        anvil: FallingBlock,
        sender: Player,
    ) {
        val blockData = plugin.server.createBlockData(Material.ANVIL) as Directional

        anvil.blockData =
            blockData.apply {
                facing =
                    if (sender.facing.modZ != 0) {
                        BlockFace.EAST
                    } else {
                        BlockFace.SOUTH
                    }
            }

        // Todo: extract to a generic function "setSender"
        anvil.persistentDataContainer.set(
            NamespacedKey(plugin, SENDER_TAG),
            PersistentDataType.STRING,
            sender.uniqueId.toString(),
        )

        // Damaged is done in the EntityChangeBlockEvent
        anvil.setHurtEntities(false)
        anvil.velocity = Vector(0.0, AnvilAbilityAttributes.FALL_SPEED_MODIFIER, 0.0)
    }

    private fun getSenderOf(entity: Entity): Player? {
        val senderId =
            entity.persistentDataContainer.get(
                NamespacedKey(plugin, SENDER_TAG),
                PersistentDataType.STRING,
            ) ?: return null
        val sender = plugin.server.getPlayer(UUID.fromString(senderId))

        if (sender == null) {
            return null
        }

        return sender
    }
}
