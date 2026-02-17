package me.davidgomes.demo.heroes.butcher

import me.davidgomes.demo.heroes.butcher.ButcherItems.anvilDropItem
import me.davidgomes.demo.heroes.getSenderOf
import me.davidgomes.demo.heroes.setEntitySender
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Directional
import org.bukkit.entity.Damageable
import org.bukkit.entity.EntityType
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import utils.hasBlocksBelow
import utils.isNotRightClick
import java.util.logging.Logger
import kotlin.math.roundToInt

class AnvilDropEventHandler(
    val plugin: Plugin,
    val logger: Logger,
) : Listener {
    @EventHandler
    fun onPlayerRightClickAnvil(evt: PlayerInteractEvent) {
        if (evt.isNotRightClick()) return
        if (anvilDropItem isNotTheSame evt.item) return
        // TODO: verify if player is in arena

        logger.info("Player ${evt.player.name} casted the anvil ability")

        // Stop interaction when it's an ability cast (e.g. to prevent placing the anvil or right-clicking on a chest)
        evt.isCancelled = true

        with(AnvilAbilityAttributes) {
            val blocksInSight = evt.player.getLineOfSight(null, MAX_CAST_DISTANCE)
            val blockInSight = blocksInSight.firstOrNull { !it.isEmpty }

            if (blockInSight == null) {
                logger.fine(
                    "Player ${evt.player.name} tried to cast anvil drop" +
                        " but there were no blocks in sight, cancelling ability",
                )
                return
            }

            val spawnLocation = blockInSight.location.add(0.0, FALL_HEIGHT, 0.0)

            // probably when the player is dead
            if (spawnLocation.world == null) {
                return
            }

            if (hasBlocksBelow(spawnLocation, FALL_HEIGHT.roundToInt())) {
                logger.fine(
                    "Player ${evt.player.name} tried to cast anvil drop but" +
                        " there are blocks below the spawn location, cancelling ability",
                )
                return
            }

            spawnFallingAnvil(spawnLocation, evt.player)
        }
    }

    @EventHandler
    fun onAnvilHit(evt: EntityChangeBlockEvent) {
        if (evt.entity.type != EntityType.FALLING_BLOCK || evt.block.type != Material.AIR) return

        val fallingBlock = evt.entity as FallingBlock

        if (fallingBlock.blockData.material != anvilDropItem.material) {
            println("hmm")
            return
        }

        val sender = getSenderOf(plugin, fallingBlock) ?: return

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
        val blockData = plugin.server.createBlockData(anvilDropItem.material) as Directional

        anvil.blockData =
            blockData.apply {
                facing =
                    if (sender.facing.modZ != 0) {
                        BlockFace.EAST
                    } else {
                        BlockFace.SOUTH
                    }
            }

        setEntitySender(plugin, anvil, sender)

        // Damage is done in the EntityChangeBlockEvent
        anvil.setHurtEntities(false)
        anvil.velocity = Vector(0.0, AnvilAbilityAttributes.FALL_SPEED_MODIFIER, 0.0)
    }
}
