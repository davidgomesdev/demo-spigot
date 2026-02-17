package me.davidgomes.demo.heroes.butcher

import me.davidgomes.demo.items.GameItem
import org.bukkit.Material

object ButcherItems {
    val selectorItem =
        GameItem(
            material = Material.COOKED_BEEF,
            name = "Butcher",
        )
    val meatCleaver =
        GameItem(
            material = Material.IRON_SWORD,
            name = "Meat Cleaver",
        )
    val anvilDropItem =
        GameItem(
            material = Material.CHIPPED_ANVIL,
            name = "Anvil Drop",
        )
}
