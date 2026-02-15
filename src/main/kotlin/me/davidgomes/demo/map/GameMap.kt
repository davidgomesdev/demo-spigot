package me.davidgomes.demo.map

import me.davidgomes.demo.arena.Team
import org.bukkit.Location
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.serialization.ConfigurationSerializable

typealias TeamSpawns = Map<Team, Location>

data class GameMap(
    val name: String,
    val teamSpawns: TeamSpawns,
) : ConfigurationSerializable {
    override fun serialize(): Map<String?, Any?> =
        mutableMapOf(
            "name" to name,
            "teamSpawns" to teamSpawns.entries.associate { it.key.name to it.value.serialize() },
        )

    companion object {
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun deserialize(serialized: Map<String, Any>): GameMap {
            val name = serialized["name"] as? String ?: throw InvalidConfigurationException("Missing 'name' field")
            val rawTeamSpawns =
                serialized["teamSpawns"] as? Map<Any, Any>
                    ?: throw InvalidConfigurationException("Missing 'teamSpawns' field")

            val teamSpawns =
                rawTeamSpawns.entries.associate { entry ->
                    val teamName =
                        entry.key as? String ?: throw InvalidConfigurationException("Invalid team name: ${entry.key}")

                    val team =
                        try {
                            Team.valueOf(teamName)
                        } catch (e: IllegalArgumentException) {
                            throw InvalidConfigurationException("Invalid team name: $teamName", e)
                        }

                    val location =
                        Location.deserialize(
                            entry.value as? Map<String, Any>
                                ?: throw InvalidConfigurationException("Invalid location for team $teamName"),
                        )

                    team to location
                }

            return GameMap(name, teamSpawns)
        }
    }
}
