package me.davidgomes.demo.heroes.butcher

import me.davidgomes.demo.hasBlocksBelow
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Directional
import org.bukkit.entity.Damageable
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import java.util.UUID
import java.util.logging.Logger
import kotlin.math.roundToInt

private const val senderTag = "sender"

class AnvilDropEventHandler(val plugin: Plugin, val logger: Logger) : Listener {

    @EventHandler
    fun onPlayerRightClickAnvil(evt: PlayerInteractEvent) {
        if (evt.item?.type != Material.ANVIL) return
        if (!(evt.action == Action.RIGHT_CLICK_AIR || evt.action == Action.RIGHT_CLICK_BLOCK)) return

        logger.fine("Player ${evt.player.name} casted the anvil ability")

        // Stop interaction when it's an ability cast (e.g. to prevent placing the anvil or right-clicking on a chest)
        evt.isCancelled = true

        with(AnvilAbilityAttributes) {
            val blocksInSight = evt.player.getLineOfSight(null, MAX_CAST_DISTANCE)
            val blockInSight = blocksInSight.firstOrNull { !it.isEmpty } ?: return

            val spawnLocation = blockInSight.location.add(0.0, FALL_HEIGHT, 0.0)

            if (hasBlocksBelow(spawnLocation, FALL_HEIGHT.roundToInt())) return

            spawnFallingAnvil(spawnLocation, evt.player)
        }
    }

    @EventHandler
    fun onAnvilHit(evt: EntityChangeBlockEvent) {
        if (evt.entity.type != EntityType.FALLING_BLOCK || evt.block.type != Material.AIR) return

        val fallingBlock = evt.entity as FallingBlock

        if (fallingBlock.blockData.material != Material.ANVIL) return

        val sender = getSenderOf(fallingBlock) ?: return
        val hitLocation = fallingBlock.location
        val world = hitLocation.world

        if (world == null) {
            logger.warning("Couldn't find world on anvil's location!")
            return
        }

        with(AnvilAbilityAttributes.Landing) {
            with(AOE) {
                world.getNearbyEntities(hitLocation, x, y, z).filterIsInstance<Damageable>()
                    .forEach {
                        it.damage(DROP_DAMAGE, sender)
                    }
            }
            world.playEffect(hitLocation, EFFECT, null)
            world.playSound(hitLocation, SOUND, VOLUME, pitch)
        }

        evt.isCancelled = true
    }

    private fun spawnFallingAnvil(spawnLocation: Location, sender: Player) {
        val world = spawnLocation.world

        // probably when the player is dead
        if (world == null) {
            logger.warning("Player ${sender.name} is in no world!?")
            return
        }

        val fallingAnvil = world.spawn(spawnLocation, FallingBlock::class.java) {
            it.blockData = (Material.ANVIL.createBlockData() as Directional).apply {
                facing = if (sender.facing.modZ != 0) {
                    BlockFace.EAST
                } else {
                    BlockFace.SOUTH
                }
            }

            it.persistentDataContainer.set(
                NamespacedKey(plugin, senderTag),
                PersistentDataType.STRING,
                sender.uniqueId.toString()
            )
        }

        // Damaged is done in the EntityChangeBlockEvent
        fallingAnvil.setHurtEntities(false)
        fallingAnvil.velocity = Vector(0.0, AnvilAbilityAttributes.FALL_SPEED_MODIFIER, 0.0)
    }

    private fun getSenderOf(entity: Entity): Player? {
        val senderId = entity.persistentDataContainer.get(
            NamespacedKey(plugin, senderTag),
            PersistentDataType.STRING
        ) ?: return null
        val sender = plugin.server.getPlayer(UUID.fromString(senderId))

        if (sender == null) {
            logger.info("Couldn't find player with id $senderId (left the server?)")
            return null
        }

        return sender
    }
}
