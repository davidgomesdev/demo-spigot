package me.davidgomes.demo.manual_testing

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands.argument
import io.papermc.paper.command.brigadier.Commands.literal
import me.davidgomes.demo.heroes.Hero
import org.bukkit.entity.Player
import java.util.logging.Logger

class DebugCommands(
    private val logger: Logger,
    rootCommand: LiteralArgumentBuilder<CommandSourceStack>,
) {
    val getHeroKit: LiteralArgumentBuilder<CommandSourceStack> =
        rootCommand
            .then(
                literal("debug_get_hero_kit")
                    .then(
                        argument("hero_name", StringArgumentType.greedyString())
                            .executes { ctx ->
                                val heroName = StringArgumentType.getString(ctx, "hero_name")
                                val playerCreating = ctx.source.sender

                                if (playerCreating !is Player) {
                                    logger.warning("Only players can get hero kits")
                                    return@executes Command.SINGLE_SUCCESS
                                }

                                val hero = Hero.list.firstOrNull { it.name.lowercase() == heroName.lowercase() }

                                if (hero == null) {
                                    playerCreating.sendMessage("No hero found with name $heroName")
                                    return@executes Command.SINGLE_SUCCESS
                                }

                                hero.setHeroItems(playerCreating.inventory)
                                playerCreating.sendMessage("Granted hero kit for hero $heroName")

                                logger.info("Player ${playerCreating.name} started creating map with name $heroName")

                                Command.SINGLE_SUCCESS
                            },
                    ),
            )
}