package me.davidgomes.demo.map

import me.davidgomes.demo.map.creation.MapCreationManager
import utils.ExYamlConfiguration
import java.util.logging.Logger

class MapManager(
    val logger: Logger,
    val config: ExYamlConfiguration
) {
    // Name to GameMap
    private var maps: Map<String, GameMap>

    init {
        maps = reloadMaps()
    }

    infix fun existsMapWithName(name: String): Boolean = maps.containsKey(name)

    fun getMapByName(name: String): GameMap? = maps[name]

    fun getAllMaps(): Collection<GameMap> = maps.values

    @Suppress("UNCHECKED_CAST")
    fun reloadMaps(): Map<String, GameMap> {
        config.reload()
        maps = (config.getList("maps", listOf<GameMap>()) as List<GameMap>)
            .associateBy { it.name }

        logger.info("Reloaded maps (${maps.size} maps loaded)")

        return maps
    }

    fun addMap(creationSession: MapCreationManager.MapCreationSession): GameMap {
        val newMap = creationSession.toGameMap()

        config.setAndSave("maps", (maps.values + newMap))
        logger.info("Added map '${creationSession.mapName}'")

        reloadMaps()

        return newMap
    }
}