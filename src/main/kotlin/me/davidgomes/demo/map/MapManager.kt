package me.davidgomes.demo.map

import me.davidgomes.demo.map.creation.MapCreationManager
import utils.ExYamlConfiguration
import java.util.logging.Logger

class MapManager(
    val logger: Logger,
    val config: ExYamlConfiguration,
) {
    // Name to GameMap
    // Needs to be initialized empty and only loaded after enablement
    private var maps: Map<String, GameMap> = emptyMap()

    infix fun existsMapWithName(name: String): Boolean = maps.containsKey(name)

    fun getMapByName(name: String): GameMap? = maps[name]

    fun getAllMaps(): Collection<GameMap> = maps.values

    @Suppress("UNCHECKED_CAST")
    fun reloadMaps(): Map<String, GameMap> {
        config.reload()
        maps =
            (config.getList("maps", listOf<Map<String, *>>()) as List<Map<String, *>>)
                .map { GameMap.deserialize(it) }
                .associateBy { it.name }

        logger.info("Reloaded maps (${maps.size} maps loaded)")

        return maps
    }

    fun addMap(creationSession: MapCreationManager.MapCreationSession): GameMap {
        val newMap = creationSession.toGameMap()

        config.setAndSave("maps", (maps.values + newMap).map(GameMap::serialize))
        logger.info("Added map '${creationSession.mapName}'")

        reloadMaps()

        return newMap
    }
}
