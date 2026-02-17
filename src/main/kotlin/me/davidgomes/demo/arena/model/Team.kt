package me.davidgomes.demo.arena.model

import org.bukkit.Material

enum class Team(
    val spawnItemMaterial: Material,
) {
    Yellow(Material.YELLOW_CANDLE),
    Blue(Material.BLUE_CANDLE),
    ;

    companion object {
        val count = entries.count()
    }
}
