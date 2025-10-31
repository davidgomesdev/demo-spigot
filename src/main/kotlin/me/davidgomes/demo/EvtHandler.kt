package me.davidgomes.demo

import org.bukkit.Effect
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Directional
import org.bukkit.entity.Damageable
import org.bukkit.entity.EntityType
import org.bukkit.entity.FallingBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.metadata.FixedMetadataValue
import java.util.UUID

class EvtHandler : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerRightClickAnvil(evt: PlayerInteractEvent) {
        if (evt.item?.type != Material.ANVIL) return
        if (evt.action != Action.RIGHT_CLICK_AIR && evt.action != Action.RIGHT_CLICK_BLOCK) return

        evt.isCancelled = true

        val blocksInSight = evt.player.getLineOfSight(null, 20)
        val blockInSight = blocksInSight.firstOrNull { !it.isEmpty } ?: return

        val spawnLocation = blockInSight.location.add(0.0, 5.0, 0.0)

        if (hasBlocksBelow(spawnLocation, 5)) return

        val world = spawnLocation.world

        if (world == null) {
            log.warning("Player ${evt.player.name} is in no world!?")
            return
        }

        val createBlockData = Material.ANVIL.createBlockData() as Directional

        if (evt.player.facing.modZ != 0) {
            createBlockData.facing = BlockFace.EAST
        } else {
            createBlockData.facing = BlockFace.SOUTH
        }

        val fallingAnvil = world.spawnFallingBlock(spawnLocation, createBlockData)

        fallingAnvil.setMetadata("sender", FixedMetadataValue(plugin, evt.player.uniqueId.toString()))
        fallingAnvil.setHurtEntities(true)
    }

    @EventHandler
    fun onAnvilHit(evt: EntityChangeBlockEvent) {
        if (evt.entity.type != EntityType.FALLING_BLOCK || evt.block.type != Material.AIR) return

        val fallingBlock = evt.entity as FallingBlock

        if (fallingBlock.blockData.material != Material.ANVIL) return

        val senderIdMetadata =
            evt.entity.getMetadata("sender").firstOrNull { it.owningPlugin?.name == plugin.name } ?: return

        val senderId = UUID.fromString(senderIdMetadata.asString())
        val sender = plugin.server.getPlayer(senderId)

        if (sender == null) {
            log.warning("Couldn't find player with id $senderId")
            return
        }

        val hitLocation = evt.entity.location
        val world = hitLocation.world

        if (world == null) {
            log.warning("Couldn't find world on anvil's location!")
        }

        world!!.getNearbyEntities(hitLocation, 1.0, 1.0, 1.0).filterIsInstance<Damageable>()
            .forEach {
                it.damage(5.0, sender)
            }

        world.playEffect(hitLocation, Effect.ANVIL_LAND, null)
        world.playSound(hitLocation, Sound.BLOCK_ANVIL_HIT, 100.0f, 1.0f)

        evt.isCancelled = true
    }
}
