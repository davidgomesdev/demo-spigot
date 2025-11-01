package me.davidgomes.demo.heroes.butcher

import me.davidgomes.demo.hasBlocksBelow
import me.davidgomes.demo.log
import me.davidgomes.demo.plugin
import org.bukkit.Effect
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
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
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.util.Vector
import java.util.UUID

private object AnvilAbilityAttributes {
    const val MAX_CAST_DISTANCE = 20
    const val CAST_RANGE = 5
    const val FALL_HEIGHT = 5.0
    const val DROP_DAMAGE = 5.0
    val AOE: Vector = Vector(1.0, 1.0, 1.0)

    object Land {
        val EFFECT: Effect = Effect.ANVIL_LAND
        val SOUND: Sound = Sound.BLOCK_ANVIL_HIT
        const val VOLUME = 100.0f
        const val PITCH = 1.0f
    }
}

private const val senderTag = "sender"

class ButcherEventHandler : Listener {

    @EventHandler
    fun onPlayerRightClickAnvil(evt: PlayerInteractEvent) {
        if (evt.item == null || evt.item?.type != Material.ANVIL) return
        if (evt.action != Action.RIGHT_CLICK_AIR && evt.action != Action.RIGHT_CLICK_BLOCK) return

        evt.isCancelled = true

        with(AnvilAbilityAttributes) {
            val blocksInSight = evt.player.getLineOfSight(null, MAX_CAST_DISTANCE)
            val blockInSight = blocksInSight.firstOrNull { !it.isEmpty } ?: return

            val spawnLocation = blockInSight.location.add(0.0, FALL_HEIGHT, 0.0)

            if (hasBlocksBelow(spawnLocation, CAST_RANGE)) return

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
            log.warning("Couldn't find world on anvil's location!")
            return
        }

        with(AnvilAbilityAttributes.AOE) {
            world.getNearbyEntities(hitLocation, x, y, z).filterIsInstance<Damageable>()
                .forEach {
                    it.damage(AnvilAbilityAttributes.DROP_DAMAGE, sender)
                }
        }

        with(AnvilAbilityAttributes.Land) {
            world.playEffect(hitLocation, EFFECT, null)
            world.playSound(hitLocation, SOUND, VOLUME, PITCH)
        }

        evt.isCancelled = true
    }

    private fun spawnFallingAnvil(spawnLocation: Location, sender: Player) {
        val world = spawnLocation.world

        if (world == null) {
            log.warning("Player ${sender.name} is in no world!?")
            return
        }

        val createBlockData = Material.ANVIL.createBlockData() as Directional

        if (sender.facing.modZ != 0) {
            createBlockData.facing = BlockFace.EAST
        } else {
            createBlockData.facing = BlockFace.SOUTH
        }

        val fallingAnvil = world.spawnFallingBlock(spawnLocation, createBlockData)

        fallingAnvil.setMetadata(senderTag, FixedMetadataValue(plugin, sender.uniqueId.toString()))
        fallingAnvil.setHurtEntities(true)
    }

    private fun getSenderOf(entity: Entity): Player? {
        val senderIdMetadata =
            entity.getMetadata(senderTag).firstOrNull { it.owningPlugin?.name == plugin.name } ?: return null

        val senderId = UUID.fromString(senderIdMetadata.asString())
        val sender = plugin.server.getPlayer(senderId)

        if (sender == null) {
            log.warning("Couldn't find player with id $senderId")
            return null
        }

        return sender
    }
}
