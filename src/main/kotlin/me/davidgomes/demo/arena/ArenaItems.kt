package me.davidgomes.demo.arena

import me.davidgomes.demo.items.GameItem
import org.bukkit.Material

object ArenaItems {
    val join =
        GameItem(
            material = Material.DIAMOND_SWORD,
            name = "Join Arena",
        )
    val start =
        GameItem(
            material = Material.EMERALD,
            name = "Use to start Arena",
        )
}