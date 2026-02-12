package me.davidgomes.demo.heroes.butcher

import org.bukkit.Effect
import org.bukkit.Sound
import org.bukkit.util.Vector
import kotlin.random.Random

internal object AnvilAbilityAttributes {
    const val MAX_CAST_DISTANCE = 20
    const val FALL_HEIGHT = 5.0
    const val FALL_SPEED_MODIFIER = -0.5

    object Landing {
        const val DROP_DAMAGE = 5.0
        val AOE: Vector = Vector(1.0, 1.0, 1.0)
        val EFFECT: Effect = Effect.ANVIL_LAND
        val SOUND: Sound = Sound.BLOCK_ANVIL_HIT
        const val VOLUME = 100.0f
        val pitch get() = run { Random.nextFloat() }
    }
}
