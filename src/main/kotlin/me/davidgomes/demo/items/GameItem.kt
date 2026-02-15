package me.davidgomes.demo.items

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack

class GameItem(
    val material: Material,
    val name: String,
) : ItemStack(material) {
    init {
        editMeta { it.customName(Component.text(name)) }
    }

    infix fun isNotTheSame(item: Item?) = !(this isTheSame item?.itemStack)

    infix fun isNotTheSame(itemStack: ItemStack?) = !(this isTheSame itemStack)

    @Suppress("UNUSED")
    infix fun isTheSame(item: Item?) = this isTheSame item?.itemStack

    infix fun isTheSame(itemStack: ItemStack?): Boolean {
        val customName = itemStack?.itemMeta?.customName() ?: return false

        if (customName !is TextComponent) return false

        return itemStack.type == material && customName.content() == name
    }
}
