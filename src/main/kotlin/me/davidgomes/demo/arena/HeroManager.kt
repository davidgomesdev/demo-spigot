package me.davidgomes.demo.arena

import me.davidgomes.demo.heroes.Hero
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.logging.Logger

class HeroManager(
    private val plugin: Plugin,
    private val logger: Logger,
) {
    /**
     * Note: there's no need to "reset" or remove the tag, as it will be overridden when the player joins another arena
     */
    fun setHero(
        player: Player,
        hero: Hero,
    ) {
        player.persistentDataContainer.set(
            NamespacedKey(plugin, "hero"),
            PersistentDataType.STRING,
            hero.name,
        )
        logger.info("Set hero ${hero.name} for player '${player.name}'")
    }

    fun getHero(player: Player): Hero? {
        val heroName =
            player.persistentDataContainer
                .get(NamespacedKey(plugin, "hero"), PersistentDataType.STRING)

        if (heroName == null) {
            logger.warning("Player '${player.name}' does not have a hero set")
            return null
        }

        val hero = Hero.list.firstOrNull { it.name == heroName }

        if (hero == null) {
            logger.warning("Hero '$heroName' not found on player '${player.name}'")
        }

        return hero
    }
}
