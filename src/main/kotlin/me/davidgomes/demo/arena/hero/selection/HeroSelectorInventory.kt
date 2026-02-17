package me.davidgomes.demo.arena.hero.selection

import me.davidgomes.demo.heroes.Hero
import net.kyori.adventure.text.Component
import org.bukkit.Server
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class HeroSelectorInventory(
    val server: Server,
) : InventoryHolder {
    private val inventory: Inventory =
        server.createInventory(
            this,
            Hero.list.size.coerceAtLeast(9),
            Component.text("Hero Selector"),
        )

    override fun getInventory(): Inventory = inventory
}
