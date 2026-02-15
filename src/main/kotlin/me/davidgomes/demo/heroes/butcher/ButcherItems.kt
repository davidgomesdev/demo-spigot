package me.davidgomes.demo.heroes.butcher

import me.davidgomes.demo.items.GameItem
import org.bukkit.Material

object ButcherItems {
    val meatCleaver =
        GameItem(
            material = Material.IRON_SWORD,
            name = "Meat Cleaver",
        )
    val anvilDropItem =
        GameItem(
            material = Material.ANVIL,
            name = "Anvil Drop",
        )
}
