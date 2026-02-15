package me.davidgomes.demo.map.creation

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands.argument
import io.papermc.paper.command.brigadier.Commands.literal
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.util.logging.Logger

class MapCreationCommands(
    val logger: Logger,
    val manager: MapCreationManager
) {
    val createMap: LiteralCommandNode<CommandSourceStack> = literal("mg")
        .then(literal("create_map"))
        .then(argument("map_name", StringArgumentType.greedyString()))
        .executes { ctx ->
            val mapName = StringArgumentType.getString(ctx, "map_name")
            val playerCreating = ctx.source.sender

            if (playerCreating !is Player) {
                logger.warning("Only players can create maps")
                return@executes Command.SINGLE_SUCCESS
            }

            logger.info("Player ${playerCreating.name} is creating map with name $mapName")

            manager.createSession(playerCreating, mapName)

            playerCreating.sendMessage(
                Component.text(
                    "You are now in map creation mode! " +
                            "Use the spawn pickers to set the spawns for each team, " +
                            "and then use the finish creation item to finish the process."
                )
            )

            Command.SINGLE_SUCCESS
        }
        .build()
}
