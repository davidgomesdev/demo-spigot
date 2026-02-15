package utils

import org.bukkit.Location
import org.bukkit.Material
import kotlin.math.roundToInt

fun hasBlocksBelow(
    from: Location,
    range: Int,
): Boolean {
    require(from.world != null) { "Location must contain the world!" }

    val world = from.world!!

    repeat(range + 1) {
        val blockAtLocation =
            world.getBlockAt(
                from.x.roundToInt(),
                from.y.roundToInt() - it,
                from.z.roundToInt(),
            )

        if (blockAtLocation.blockData.material != Material.AIR) return true
    }

    return false
}
