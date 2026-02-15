package me.davidgomes.demo.map

import me.davidgomes.demo.arena.Team
import org.bukkit.Location
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.serialization.ConfigurationSerializable

typealias TeamSpawns = Map<Team, Location>

data class GameMap(val name: String, val teamSpawns: TeamSpawns) : ConfigurationSerializable {
    override fun serialize(): Map<String?, Any?> {
        return mutableMapOf(
            "name" to name,
            "teamSpawns" to teamSpawns.entries.associate { it.key.name to it.value.serialize() }
        )
    }

    companion object {
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun deserialize(serialized: Map<String, Any>): GameMap {
            val name = serialized["name"] as? String ?: throw InvalidConfigurationException("Missing 'name' field")
            val teamSpawns = serialized["teamSpawns"] as? TeamSpawns
                ?: throw InvalidConfigurationException("Missing 'teamSpawns' field")

            return GameMap(name, teamSpawns)
        }
    }
}
