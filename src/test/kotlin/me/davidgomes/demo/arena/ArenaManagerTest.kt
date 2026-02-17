package me.davidgomes.demo.arena

import io.mockk.*
import me.davidgomes.demo.Main
import me.davidgomes.demo.heroes.butcher.ButcherHero
import me.davidgomes.demo.heroes.setEntitySender
import me.davidgomes.demo.map.GameMap
import me.davidgomes.demo.map.MapManager
import me.davidgomes.demo.messages.ARENA_STARTED
import org.bukkit.*
import org.bukkit.entity.Entity
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.entity.FallingBlockMock
import org.mockbukkit.mockbukkit.inventory.ItemStackMock
import java.util.*
import java.util.logging.Logger
import kotlin.test.*

class ArenaManagerTest {
    lateinit var arenaManager: ArenaManager
    lateinit var server: ServerMock
    lateinit var plugin: Main
    lateinit var heroManager: HeroManager
    lateinit var mapManager: MapManager
    lateinit var previousLocationManager: PreviousLocationManager
    lateinit var world: World

    @BeforeTest
    fun setUp() {
        MockBukkit.mock()
        server = ServerMock()
        plugin = spyk(MockBukkit.load(Main::class.java))
        world = server.addSimpleWorld("test_world")

        val logger = Logger.getLogger("ArenaManagerTest")

        heroManager = spyk(HeroManager(plugin, logger))
        mapManager = mockk(relaxUnitFun = true)
        previousLocationManager =
            mockk {
                every { saveLocation(any()) } just Runs
                every { getSavedLocation(any()) } returns world.spawnLocation
            }

        every { mapManager.getAllMaps() } returns
            listOf(
                GameMap(
                    "test_map",
                    mapOf(
                        Team.Yellow to Location(server.addSimpleWorld("test_world"), 0.0, 65.0, 0.0),
                        Team.Blue to Location(server.addSimpleWorld("test_world"), 10.0, 65.0, 0.0),
                    ),
                ),
            )

        arenaManager = ArenaManager(plugin, logger, heroManager, mapManager, previousLocationManager)

        every { plugin.server } returns server
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `joinArena assigns player to a team`() {
        val player = server.addPlayer()
        val team = arenaManager.joinArena(player)

        assertTrue(arenaManager.isInArena(player))
        assertEquals(team, arenaManager.getTeam(player))
    }

    @Test
    fun `second player joins second team when there is already one of the first`() {
        val firstTeam = arenaManager.joinArena(server.addPlayer())
        val secondTeam = arenaManager.joinArena(server.addPlayer())

        assertEquals(Team.Yellow, firstTeam)
        assertEquals(Team.Blue, secondTeam)
    }

    @Test
    fun `joinArena assigns player to team with least players`() {
        repeat(3) {
            arenaManager.joinArena(server.addPlayer())
        }

        val newPlayer = server.addPlayer()
        val assignedTeam = arenaManager.joinArena(newPlayer)

        // Should be assigned to Blue since it has fewer players
        assertEquals(Team.Blue, assignedTeam)
        assertTrue(arenaManager.getPlayersInTeam(Team.Blue).contains(newPlayer))
        assertEquals(2, arenaManager.getTeamSize(Team.Blue))
        assertEquals(2, arenaManager.getTeamSize(Team.Yellow))
    }

    @Test
    fun `joinArena gives the start arena and hero selector items, sets a hero and gamemode to adventure`() {
        val player = spyk(server.addPlayer())
        val inventory = spyk(player.inventory)

        every { player.inventory } returns inventory

        arenaManager.joinArena(player)

        verify { inventory.clear() }
        verify { inventory.setItem(0, ArenaItems.heroSelector) }
        verify { inventory.setItem(1, ArenaItems.start) }
        verify { heroManager.setHero(player, any()) }
        verify { player.gameMode = GameMode.ADVENTURE }
    }

    @Test
    fun `leaveArena removes player from their team`() {
        val player = server.addPlayer()

        arenaManager.joinArena(player)
        assertTrue(arenaManager.isInArena(player))

        arenaManager.leaveArena(player)
        assertFalse(arenaManager.isInArena(player))
    }

    @Test
    fun `leaveArena handles player not in arena gracefully`() {
        val player = server.addPlayer()

        assertDoesNotThrow {
            arenaManager.leaveArena(player)
        }
        assertFalse(arenaManager.isInArena(player))
    }

    @Test
    fun `isInArena returns false for player not in arena`() {
        val player = server.addPlayer()

        assertFalse(arenaManager.isInArena(player))
    }

    @Test
    fun `isInArena returns true for player in arena`() {
        val player = server.addPlayer()

        arenaManager.joinArena(player)

        assertTrue(arenaManager.isInArena(player))
    }

    @Test
    fun `first player joins first team when no players exist`() {
        val player = server.addPlayer()
        val team = arenaManager.joinArena(player)

        assertEquals(Team.entries.first(), team)
    }

    @Nested
    inner class Match {
        @Test
        fun `startArena clears player inventory and sets state to ongoing TDM`() {
            val world = server.addSimpleWorld("test_world")
            val teamSpawns =
                mapOf(
                    Team.Yellow to Location(world, 0.0, 65.0, 0.0),
                    Team.Blue to Location(world, 10.0, 65.0, 0.0),
                )
            val gameMap = GameMap("test_map", teamSpawns)

            every { mapManager.getAllMaps() } returns listOf(gameMap)

            val player1 = spyk(server.addPlayer())
            val player2 = spyk(server.addPlayer())

            every { player1.teleport(any<Location>()) } returns true
            every { player2.teleport(any<Location>()) } returns true

            arenaManager.joinArena(player1)
            arenaManager.joinArena(player2)

            arenaManager.startArena(GameType.TeamDeathMatch)

            assertTrue(arenaManager.isMatchOnGoing())

            val state = arenaManager.getState()

            assertTrue(state is ArenaState.OnGoingTeamDeathMatch)
            assertEquals(SCORE_GOAL, state.scoreGoal)
            Team.entries.forEach { team ->
                assertNotNull(state.scoreboard[team])
                assertEquals(0, state.scoreboard[team]?.get())
            }
        }

        @Test
        fun `startArena teleports players to their team spawns`() {
            val world = server.addSimpleWorld("test_world")
            val yellowSpawn = Location(world, 0.0, 65.0, 0.0)
            val blueSpawn = Location(world, 10.0, 65.0, 0.0)
            val teamSpawns =
                mapOf(
                    Team.Yellow to yellowSpawn,
                    Team.Blue to blueSpawn,
                )
            val gameMap = GameMap("test_map", teamSpawns)

            every { mapManager.getAllMaps() } returns listOf(gameMap)

            val yellowTeamPlayer = spyk(server.addPlayer())
            val blueTeamPlayer = spyk(server.addPlayer())

            every { yellowTeamPlayer.teleport(any<Location>()) } returns true
            every { blueTeamPlayer.teleport(any<Location>()) } returns true

            arenaManager.joinArena(yellowTeamPlayer)
            arenaManager.joinArena(blueTeamPlayer)

            arenaManager.startArena(GameType.TeamDeathMatch)

            verify { yellowTeamPlayer.teleport(yellowSpawn) }
            verify { blueTeamPlayer.teleport(blueSpawn) }
            verify { yellowTeamPlayer.sendMessage(ARENA_STARTED) }
            verify { blueTeamPlayer.sendMessage(ARENA_STARTED) }
        }

        @Test
        fun `startArena uses existing hero for player if set`() {
            val world = server.addSimpleWorld("test_world")
            val teamSpawns =
                mapOf(
                    Team.Yellow to Location(world, 0.0, 65.0, 0.0),
                    Team.Blue to Location(world, 10.0, 65.0, 0.0),
                )
            val gameMap = GameMap("test_map", teamSpawns)

            every { mapManager.getAllMaps() } returns listOf(gameMap)

            val player1 = server.addPlayer()
            val player2 = server.addPlayer()

            arenaManager.joinArena(player1)
            arenaManager.joinArena(player2)

            heroManager.setHero(player1, ButcherHero)

            arenaManager.startArena(GameType.TeamDeathMatch)

            val hero = heroManager.getHero(player1)
            assertEquals(ButcherHero, hero)
        }

        @Test
        fun `startArena sets hero items to player inventory`() {
            val world = server.addSimpleWorld("test_world")
            val teamSpawns =
                mapOf(
                    Team.Yellow to Location(world, 0.0, 65.0, 0.0),
                    Team.Blue to Location(world, 10.0, 65.0, 0.0),
                )
            val gameMap = GameMap("test_map", teamSpawns)

            every { mapManager.getAllMaps() } returns listOf(gameMap)

            val player1 = server.addPlayer()
            val player2 = server.addPlayer()

            arenaManager.joinArena(player1)
            arenaManager.joinArena(player2)

            arenaManager.startArena(GameType.TeamDeathMatch)
            val hero = assertNotNull(heroManager.getHero(player1))

            assertFalse(player1.inventory.isEmpty)

            hero.items.forEachIndexed { i, item ->
                assertTrue(player1.inventory.getItem(i) == item)
            }

            assertFalse(player2.inventory.isEmpty)
        }

        @Test
        fun `isReadyToStart returns false when no players in arena`() {
            assertFalse(arenaManager.isReadyToStart())
        }

        @Test
        fun `isReadyToStart returns false when only one team has players`() {
            arenaManager.joinArena(server.addPlayer())

            assertFalse(arenaManager.isReadyToStart())
        }

        @Test
        fun `isReadyToStart returns true when all teams have players`() {
            val player1 = spyk(server.addPlayer())
            val player2 = spyk(server.addPlayer())

            every { player1.teleport(any<Location>()) } returns true
            every { player2.teleport(any<Location>()) } returns true

            arenaManager.joinArena(player1)
            arenaManager.joinArena(player2)

            assertTrue(arenaManager.isReadyToStart())
        }

        @Test
        fun `isReadyToStart returns false when match is already ongoing`() {
            val player1 = spyk(server.addPlayer())
            val player2 = spyk(server.addPlayer())

            every { player1.teleport(any<Location>()) } returns true
            every { player2.teleport(any<Location>()) } returns true

            arenaManager.joinArena(player1)
            arenaManager.joinArena(player2)

            arenaManager.startArena(GameType.TeamDeathMatch)

            assertFalse(arenaManager.isReadyToStart())
        }

        @Test
        fun `addItemToJoinArena clears inventory and adds join item`() {
            val player = spyk(server.addPlayer())

            arenaManager.addItemToJoinArena(player)

            assertEquals(ArenaItems.join, player.inventory.getItem(0))
        }

        @Test
        fun `isMatchOnGoing returns false when in lobby`() {
            assertFalse(arenaManager.isMatchOnGoing())
        }

        @Test
        fun `isMatchOnGoing returns true after starting arena`() {
            val player1 = spyk(server.addPlayer())
            val player2 = spyk(server.addPlayer())

            every { player1.teleport(any<Location>()) } returns true
            every { player2.teleport(any<Location>()) } returns true

            arenaManager.joinArena(player1)
            arenaManager.joinArena(player2)
            arenaManager.startArena(GameType.TeamDeathMatch)

            assertTrue(arenaManager.isMatchOnGoing())
        }

        @Test
        fun `match ends when team reaches score goal in TDM`() {
            val killer = spyk(server.addPlayer())
            val victim = spyk(server.addPlayer())

            every { killer.teleport(any<Location>()) } returns true
            every { victim.teleport(any<Location>()) } returns true

            arenaManager.joinArena(killer)
            arenaManager.joinArena(victim)
            arenaManager.startArena(GameType.TeamDeathMatch)

            val killerTeam = arenaManager.getTeam(killer)!!

            val stateBefore = arenaManager.getState() as ArenaState.OnGoingTeamDeathMatch

            repeat(stateBefore.scoreGoal) { iteration ->
                val stateAfter = arenaManager.getState()

                assertIs<ArenaState.OnGoingTeamDeathMatch>(stateAfter)

                arenaManager.onPlayerKilledByPlayer(victim, killer)

                assertEquals(iteration + 1, stateAfter.scoreboard[killerTeam]?.get())
            }

            val stateAfter = arenaManager.getState()

            assertEquals(ArenaState.EndedTeamDeathMatch(killerTeam), stateAfter)
            assertFalse(arenaManager.isMatchOnGoing())
        }

        @Test
        fun `both teams can score kills alternately`() {
            val yellowPlayer = server.addPlayer()
            val bluePlayer = server.addPlayer()
            arenaManager.joinArena(yellowPlayer) // Yellow team
            arenaManager.joinArena(bluePlayer) // Blue team

            val testScoreboard =
                Team.entries.associateWith {
                    java.util.concurrent.atomic
                        .AtomicInteger(0)
                }
            val world = server.addSimpleWorld("test_world")
            val testState =
                ArenaState.OnGoingTeamDeathMatch(
                    mapOf(
                        Team.Yellow to Location(world, 0.0, 65.0, 0.0),
                        Team.Blue to Location(world, 10.0, 65.0, 0.0),
                    ),
                    5,
                    testScoreboard,
                )

            val stateField = ArenaManager::class.java.getDeclaredField("state")
            stateField.isAccessible = true
            stateField.set(arenaManager, testState)

            // Yellow kills Blue
            arenaManager.onPlayerKilledByPlayer(bluePlayer, yellowPlayer)
            // Blue kills Yellow
            arenaManager.onPlayerKilledByPlayer(yellowPlayer, bluePlayer)
            // Yellow kills Blue again
            arenaManager.onPlayerKilledByPlayer(bluePlayer, yellowPlayer)

            val state = arenaManager.getState() as ArenaState.OnGoingTeamDeathMatch
            assertEquals(2, state.scoreboard[Team.Yellow]?.get())
            assertEquals(1, state.scoreboard[Team.Blue]?.get())
        }

        @Test
        fun `onPlayerKilledByPlayer does nothing when match is not ongoing`() {
            val killer = server.addPlayer()
            val victim = server.addPlayer()

            arenaManager.joinArena(killer)
            arenaManager.joinArena(victim)

            // Should not throw even when match is not ongoing
            assertDoesNotThrow {
                arenaManager.onPlayerKilledByPlayer(victim, killer)
            }

            assertTrue(arenaManager.getState() is ArenaState.Lobby)
            assertFalse(arenaManager.isMatchOnGoing())
        }

        @Test
        fun `onPlayerKilledByPlayer ignores if killer is the same as victim`() {
            val player = spyk(server.addPlayer())
            val other = spyk(server.addPlayer())

            every { player.teleport(any<Location>()) } returns true
            every { other.teleport(any<Location>()) } returns true

            arenaManager.joinArena(player)
            arenaManager.joinArena(other)

            arenaManager.startArena(GameType.TeamDeathMatch)

            val stateBefore = arenaManager.getState() as ArenaState.OnGoingTeamDeathMatch
            val scoreBefore = stateBefore.scoreboard[Team.Yellow]?.get()

            assertDoesNotThrow {
                arenaManager.onPlayerKilledByPlayer(player, player)
            }
            assertTrue(arenaManager.isMatchOnGoing())

            val stateAfter = arenaManager.getState() as ArenaState.OnGoingTeamDeathMatch

            assertEquals(scoreBefore, stateAfter.scoreboard[Team.Yellow]?.get())
        }

        @Test
        fun `onPlayerKilledByPlayer ignores if players are on same team`() {
            val firstPlayerYellow = spyk(server.addPlayer())
            val secondPlayerBlue = spyk(server.addPlayer())
            val thirdPlayerYellow = spyk(server.addPlayer())

            every { firstPlayerYellow.teleport(any<Location>()) } returns true
            every { secondPlayerBlue.teleport(any<Location>()) } returns true
            every { thirdPlayerYellow.teleport(any<Location>()) } returns true

            // Join 3 players so 2 are on the same team
            arenaManager.joinArena(firstPlayerYellow)
            arenaManager.joinArena(secondPlayerBlue)
            arenaManager.joinArena(thirdPlayerYellow)

            arenaManager.startArena(GameType.TeamDeathMatch)

            val stateBefore = arenaManager.getState() as ArenaState.OnGoingTeamDeathMatch
            val yellowScoreBefore = stateBefore.scoreboard[Team.Yellow]?.get()
            val blueScoreBefore = stateBefore.scoreboard[Team.Blue]?.get()

            // player1 and player3 are on the same team (Yellow)
            assertDoesNotThrow {
                arenaManager.onPlayerKilledByPlayer(firstPlayerYellow, thirdPlayerYellow)
            }

            // State should still be ongoing TDM with unchanged scores
            assertTrue(arenaManager.isMatchOnGoing())
            val stateAfter = arenaManager.getState() as ArenaState.OnGoingTeamDeathMatch
            assertEquals(yellowScoreBefore, stateAfter.scoreboard[Team.Yellow]?.get())
            assertEquals(blueScoreBefore, stateAfter.scoreboard[Team.Blue]?.get())
        }

        @Test
        fun `onPlayerKilledByPlayer increments score for executor team in TDM`() {
            val killer = spyk(server.addPlayer())
            val victim = spyk(server.addPlayer())

            every { killer.teleport(any<Location>()) } returns true
            every { victim.teleport(any<Location>()) } returns true

            arenaManager.joinArena(killer)
            arenaManager.joinArena(victim)
            arenaManager.startArena(GameType.TeamDeathMatch)

            val killerTeam = arenaManager.getTeam(killer)!!

            val stateBefore = arenaManager.getState() as ArenaState.OnGoingTeamDeathMatch
            val scoreBefore = stateBefore.scoreboard[killerTeam]?.get() ?: 0

            arenaManager.onPlayerKilledByPlayer(victim, killer)

            // Match should still be ongoing (only 1 kill, not enough to win)
            assertTrue(arenaManager.isMatchOnGoing())
            val stateAfter = arenaManager.getState() as ArenaState.OnGoingTeamDeathMatch
            assertEquals(scoreBefore + 1, stateAfter.scoreboard[killerTeam]?.get())
        }

        @Test
        fun `onPlayerKilledByEntity does nothing when sender cannot be identified`() {
            val victim = spyk(server.addPlayer())

            every { victim.teleport(any<Location>()) } returns true

            arenaManager.joinArena(victim)
            arenaManager.joinArena(server.addPlayer())
            arenaManager.startArena(GameType.TeamDeathMatch)

            val stateBefore = arenaManager.getState() as ArenaState.OnGoingTeamDeathMatch
            val yellowScoreBefore = stateBefore.scoreboard[Team.Yellow]?.get()
            val blueScoreBefore = stateBefore.scoreboard[Team.Blue]?.get()

            val entity = mockk<Entity>(relaxed = true)
            val pdc = mockk<PersistentDataContainer>()

            every { entity.persistentDataContainer } returns pdc
            every { pdc.get(any<NamespacedKey>(), eq(PersistentDataType.STRING)) } returns null

            assertDoesNotThrow {
                arenaManager.onPlayerKilledByEntity(victim, entity)
            }

            // Scores should remain unchanged
            val stateAfter = arenaManager.getState() as ArenaState.OnGoingTeamDeathMatch
            assertEquals(yellowScoreBefore, stateAfter.scoreboard[Team.Yellow]?.get())
            assertEquals(blueScoreBefore, stateAfter.scoreboard[Team.Blue]?.get())
        }

        @Test
        fun `onPlayerKilledByEntity increments score when sender found`() {
            val killer = spyk(server.addPlayer())
            val victim = spyk(server.addPlayer())

            every { killer.teleport(any<Location>()) } returns true
            every { victim.teleport(any<Location>()) } returns true

            arenaManager.joinArena(killer)
            arenaManager.joinArena(victim)
            arenaManager.startArena(GameType.TeamDeathMatch)

            val killerTeam = arenaManager.getTeam(killer)!!

            val entity = FallingBlockMock(server, UUID.randomUUID())

            setEntitySender(plugin, entity, killer)

            assertDoesNotThrow {
                arenaManager.onPlayerKilledByEntity(victim, entity)
            }

            val stateAfter = assertIs<ArenaState.OnGoingTeamDeathMatch>(arenaManager.getState())

            assertEquals(1, stateAfter.scoreboard[killerTeam]?.get())
            assertEquals(0, stateAfter.scoreboard[arenaManager.getTeam(victim)]?.get())
        }
    }

    @Test
    fun `getTeam returns null for player not in arena`() {
        val player = server.addPlayer()

        assertNull(arenaManager.getTeam(player))
    }

    @Test
    fun `getTeam returns correct team for player in arena`() {
        val player = server.addPlayer()
        val expectedTeam = arenaManager.joinArena(player)

        assertEquals(expectedTeam, arenaManager.getTeam(player))
    }

    @Test
    fun `getTeamSize returns zero for empty team`() {
        assertEquals(0, arenaManager.getTeamSize(Team.Yellow))
        assertEquals(0, arenaManager.getTeamSize(Team.Blue))
    }

    @Test
    fun `getTeamSize returns correct count after players join`() {
        arenaManager.joinArena(server.addPlayer())

        assertEquals(1, arenaManager.getTeamSize(Team.Yellow))
        assertEquals(0, arenaManager.getTeamSize(Team.Blue))
    }

    @Test
    fun `getPlayersInTeam returns empty list for empty team`() {
        assertTrue(arenaManager.getPlayersInTeam(Team.Yellow).isEmpty())
    }

    @Test
    fun `getPlayersInTeam returns correct players`() {
        val player = server.addPlayer()
        arenaManager.joinArena(player)

        val yellowPlayers = arenaManager.getPlayersInTeam(Team.Yellow)

        assertEquals(1, yellowPlayers.size)
        assertEquals(player, yellowPlayers.first())

        val bluePlayers = arenaManager.getPlayersInTeam(Team.Blue)

        assertEquals(0, bluePlayers.size)
    }

    @Nested
    inner class SetHeroByItem {
        @Test
        fun `returns null when player is not in arena`() {
            val player = server.addPlayer()
            val heroItem = ButcherHero.selectorItem

            val result = assertDoesNotThrow { arenaManager.setHeroByItem(player, heroItem) }

            assertNull(result)
        }

        @Test
        fun `returns null when item is not a valid hero selector`() {
            val player = server.addPlayer()
            arenaManager.joinArena(player)

            val invalidItem = ItemStackMock(Material.DIAMOND_SWORD)

            val result = assertDoesNotThrow { arenaManager.setHeroByItem(player, invalidItem) }

            assertNull(result)
        }

        @Test
        fun `sets hero when item is valid hero selector and player is in arena`() {
            val player = server.addPlayer()
            arenaManager.joinArena(player)

            val heroItem = ButcherHero.selectorItem

            val result = arenaManager.setHeroByItem(player, heroItem)

            assertNotNull(result)
            assertEquals(ButcherHero, result)
        }

        @Test
        fun `calls heroManager setHero when item is valid`() {
            val player = server.addPlayer()
            arenaManager.joinArena(player)

            val heroItem = ButcherHero.selectorItem

            arenaManager.setHeroByItem(player, heroItem)

            verify { heroManager.setHero(player, ButcherHero) }
        }

        @Test
        fun `does not call heroManager setHero when player not in arena`() {
            val player = server.addPlayer()
            val heroItem = ButcherHero.selectorItem

            assertDoesNotThrow { arenaManager.setHeroByItem(player, heroItem) }

            verify(exactly = 0) { heroManager.setHero(any(), any()) }
        }

        @Test
        fun `does not call heroManager setHero when item is invalid`() {
            val player = server.addPlayer()
            arenaManager.joinArena(player)

            val invalidItem = ItemStackMock(Material.DIAMOND_SWORD)

            arenaManager.setHeroByItem(player, invalidItem)

            // Only the initial setHero from joinArena
            verify(exactly = 1) { heroManager.setHero(player, any()) }
        }
    }
}
