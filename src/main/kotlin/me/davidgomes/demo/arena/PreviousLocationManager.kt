package me.davidgomes.demo.arena

import me.davidgomes.demo.pdc.LocationDataType
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.logging.Logger

class PreviousLocationManager(
    private val plugin: Plugin,
    private val logger: Logger,
) {
    val pdcKey: String = "previous_location"

    /**
     * Note: there's no need to "reset" or remove the tag, as it will be overridden when the player joins another arena
     */
    fun saveLocation(player: Player) {
        player.persistentDataContainer.set(
            NamespacedKey(plugin, pdcKey),
            LocationDataType,
            player.location,
        )
        logger.info("Set previous location for player '${player.name}'")
    }

    fun getSavedLocation(player: Player): Location? {
        val previousLocation =
            player.persistentDataContainer
                .get(NamespacedKey(plugin, pdcKey), LocationDataType)

        if (previousLocation == null) {
            logger.warning("Player '${player.name}' does not have a location saved")
            return null
        }

        return previousLocation
    }
}
