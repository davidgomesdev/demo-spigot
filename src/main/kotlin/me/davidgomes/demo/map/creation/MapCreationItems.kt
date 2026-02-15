package me.davidgomes.demo.map.creation

import me.davidgomes.demo.arena.Team
import me.davidgomes.demo.items.InteractableItem
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object MapCreationItems {
    val finishCreation = InteractableItem(Material.EMERALD_BLOCK, "Finish Map Creation")
    val spawnPickers: Map<Team, InteractableItem> =
        Team.entries.associateWith { InteractableItem(it.spawnItemMaterial, "Spawn Picker for Team ${it.name}") }

    fun getTeamFromSpawnPicker(item: ItemStack): Team? =
        spawnPickers.entries
            .firstOrNull { it.value isTheSame item }
            ?.key
}
