package me.davidgomes.demo.heroes

import me.davidgomes.demo.heroes.butcher.ButcherHero
import org.bukkit.inventory.ItemStack

/**
 * @param items The items that the hero has in their inventory (ORDERED!)
 */
abstract class Hero(val name: String, val items: List<ItemStack>) {
    companion object {
        val list = listOf(
            ButcherHero,
        )

        fun getByName(name: String) = list.firstOrNull { it.name == name }
    }
}
