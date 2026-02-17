package me.davidgomes.demo.arena

import me.davidgomes.demo.arena.hero.selection.HeroManager
import me.davidgomes.demo.arena.model.ArenaState
import me.davidgomes.demo.arena.model.ArenaState.OnGoingTeamDeathMatch
import me.davidgomes.demo.arena.model.DEFAULT_SCORE_GOAL
import me.davidgomes.demo.arena.model.GameType
import me.davidgomes.demo.arena.model.Team
import me.davidgomes.demo.heroes.Hero
import me.davidgomes.demo.heroes.getSenderOf
import me.davidgomes.demo.map.GameMap
import me.davidgomes.demo.map.MapManager
import me.davidgomes.demo.messages.ARENA_STARTED
import me.davidgomes.demo.messages.JOINED_ARENA
import me.davidgomes.demo.messages.YOU_LOST
import me.davidgomes.demo.messages.YOU_WON
import org.bukkit.GameMode
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import utils.ExYamlConfiguration
import java.util.logging.Logger

// TODO: NOT THREAD-SAFE, needs a mutex on write
class ArenaManager(
    private val plugin: Plugin,
    private val logger: Logger,
    private val heroManager: HeroManager,
    private val mapManager: MapManager,
    private val previousLocationManager: PreviousLocationManager,
    private val arenaConfig: ExYamlConfiguration,
    /**
     * TODO: the best way to have this compatible with FFA,
     *      is probably to store only a list of players and then assign them to teams (in TDM) when the game starts, in ArenaState
     *      that also fixes one team getting more players when one leaves
     */
    private val players: MutableMap<Team, MutableList<Player>> =
        Team.entries
            .associateWith { mutableListOf<Player>() }
            .toMutableMap(),
    // Only one arena state, as we aren't many anyway :P
    private var state: ArenaState = ArenaState.Lobby,
) {
    fun startArena(gameType: GameType) {
        val map = mapManager.getAllMaps().random()

        state =
            when (gameType) {
                GameType.TeamDeathMatch ->
                    OnGoingTeamDeathMatch(
                        map.teamSpawns,
                        arenaConfig
                            .getInt("tdm.score_goal", DEFAULT_SCORE_GOAL)
                            .also { scoreGoal ->
                                logger.info("Using score goal of $scoreGoal for TDM matches")
                            },
                    )

                GameType.FreeForAll -> throw NotImplementedError("FFA is not implemented yet")
                GameType.CaptureTheFlag -> throw NotImplementedError("CTF is not implemented yet")
            }

        preparePlayersForMatch(map)
    }

    private fun preparePlayersForMatch(map: GameMap) {
        players.entries.forEach { teamPlayers ->
            val teamSpawn = map.teamSpawns[teamPlayers.key]!!

            teamPlayers.value.forEach { player ->
                player.setRespawnLocation(teamSpawn, true)
                player.sendMessage(ARENA_STARTED)
                player.teleport(teamSpawn)
                giveHeroItemsToPlayer(player)
            }
        }
    }

    private fun giveHeroItemsToPlayer(player: Player) {
        val hero = heroManager.getHero(player)

        if (hero == null) {
            logger.severe("Player '${player.name}' has no hero set, skipping giving hero items")
            return
        }

        hero.setHeroItems(player.inventory)
    }

    fun isReadyToStart(): Boolean =
        when (state) {
            is ArenaState.Lobby -> players.values.all { it.isNotEmpty() }
            else -> false
        }

    fun addItemToJoinArena(player: Player) {
        player.inventory.apply {
            clear()
            setItem(0, ArenaItems.join)
        }
    }

    fun joinArena(player: Player): Team {
        val team = players.minBy { it.value.size }.key

        players[team]?.add(player) ?: players.put(team, mutableListOf(player))

        player.gameMode = GameMode.ADVENTURE

        player.inventory.clear()
        player.inventory.setItem(0, ArenaItems.heroSelector)
        player.inventory.setItem(1, ArenaItems.start)

        heroManager.setHero(player, Hero.list.random())
        previousLocationManager.saveLocation(player)
        player.sendMessage(JOINED_ARENA)

        return team
    }

    fun setHeroByItem(
        player: Player,
        heroSelectorItem: ItemStack,
    ): Hero? {
        if (!isInArena(player)) {
            logger.warning("Tried setting hero for player '${player.name}' but they are not in any arena")
            return null
        }

        val hero = Hero.from(heroSelectorItem) ?: return null

        heroManager.setHero(player, hero)

        return hero
    }

    /*
     * If the player is not in any team, it logs a warning and does nothing.
     */
    fun leaveArena(player: Player) {
        val team = getTeam(player)

        if (team == null) {
            logger.warning("Tried removing player '${player.name}' but they are not in any team")
            return
        }

        players[team]?.remove(player)
        addItemToJoinArena(player)
    }

    fun getPlayersInArena(): List<Player> = players.values.flatten()

    fun getState(): ArenaState = state

    fun isMatchOnGoing(): Boolean = state.isOnGoing

    infix fun isInArena(player: Player): Boolean = getTeam(player) != null

    fun getTeam(player: Player): Team? = Team.entries.firstOrNull { players[it]?.contains(player) == true }

    fun getTeamSize(team: Team): Int = players[team]?.size ?: 0

    fun getPlayersInTeam(team: Team): List<Player> = players[team]?.toList() ?: emptyList()

    fun onPlayerKilledByEntity(
        deadPlayer: Player,
        executor: Entity,
    ) {
        val sender = getSenderOf(plugin, executor)

        if (sender == null) {
            logger.warning(
                "Player '${deadPlayer.name}' was killed by an entity" +
                    " (${executor.type}) in an arena match, but the sender could not be identified, ignoring",
            )
            return
        }

        logger.info("Player '${deadPlayer.name}' was killed by ${executor.type} in the arena match")

        onPlayerKilledByPlayer(deadPlayer, sender)
    }

    fun onPlayerKilledByPlayer(
        deadPlayer: Player,
        executor: Player,
    ) {
        if (!isMatchOnGoing()) {
            logger.warning(
                "Player '${executor.name}' killed player '${deadPlayer.name}' " +
                    "but the match is not on going, this should be avoided",
            )
            return
        }

        val deathTeam = getTeam(deadPlayer) ?: return
        val executorTeam = getTeam(executor) ?: return

        if (deadPlayer == executor) {
            logger.warning("Player '${deadPlayer.name}' killed themselves, ignoring")
            return
        }

        if (deathTeam == executorTeam) {
            logger.warning("Player '${executor.name}' killed a teammate, ignoring")
            return
        }

        logger.info("Player '${executor.name}' killed player '${deadPlayer.name}' in an arena match")

        when (val currentState = state) {
            is OnGoingTeamDeathMatch -> {
                val teamWinner = currentState.scoreKill(executorTeam) ?: return

                logger.info("Team '${teamWinner.name}' won the TDM arena")
                sendFinishMatchMessage(teamWinner)

                players.values.flatten().forEach { player ->
                    player.inventory.clear()
                    teleportPlayerToOriginalLocation(player)
                }

                state = ArenaState.EndedTeamDeathMatch(teamWinner)
            }

            else -> throw NotImplementedError(
                "Game type ${currentState::class} not yet implemented",
            )
        }
    }

    private fun teleportPlayerToOriginalLocation(player: Player) {
        val savedLocation = previousLocationManager.getSavedLocation(player)

        // This is nullable, so it's not a problem
        player.respawnLocation = savedLocation

        savedLocation?.also(player::teleport)
            ?: logger.warning(
                "Player '${player.name}' does not have a saved location" +
                    " to teleport to after the match ended",
            )
    }

    private fun sendFinishMatchMessage(executorTeam: Team) {
        players[executorTeam]!!.forEach { player -> player.sendMessage(YOU_WON) }
        players.entries
            .filterNot { it.key != executorTeam }
            .forEach { (_, teamPlayers) ->
                teamPlayers.forEach { player -> player.sendMessage(YOU_LOST) }
            }
    }
}
