package me.davidgomes.demo.items

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack

class InteractableItem(
    val material: Material,
    val name: String,
) : ItemStack(material) {

    init {
        editMeta { it.customName(Component.text(name)) }
    }

    infix fun isNotTheSame(item: Item?) = !(this isTheSame item?.itemStack)

    infix fun isNotTheSame(itemStack: ItemStack?) = !(this isTheSame itemStack)

    infix fun isTheSame(item: Item?) = this isTheSame item?.itemStack

    infix fun isTheSame(itemStack: ItemStack?): Boolean {
        println("Comparing $this with $itemStack")
        val customName = itemStack?.itemMeta?.customName() ?: return false
        println("has custom name: $customName")

        if (customName !is TextComponent) return false
        println("custom name is text component: ${customName.content()}")

        return itemStack.type == material && customName.content() == name
    }
}