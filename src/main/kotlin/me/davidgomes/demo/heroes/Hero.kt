package me.davidgomes.demo.heroes

import me.davidgomes.demo.heroes.butcher.ButcherHero
import me.davidgomes.demo.items.GameItem
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

/**
 * @param items The items that the hero has in their inventory (ORDERED!)
 */
abstract class Hero(
    val name: String,
    val selectorItem: GameItem,
    val items: List<ItemStack>,
) {
    companion object {
        val list =
            listOf(
                ButcherHero,
            )

        fun from(heroSelectorItem: ItemStack): Hero? = list.firstOrNull { it.selectorItem isTheSame heroSelectorItem }
    }

    fun setHeroItems(playerInventory: PlayerInventory) {
        playerInventory.clear()
        items.forEachIndexed { index, itemStack -> playerInventory.setItem(index, itemStack) }
    }
}
