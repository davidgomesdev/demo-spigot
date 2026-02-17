package me.davidgomes.demo.map.creation

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands.argument
import io.papermc.paper.command.brigadier.Commands.literal
import me.davidgomes.demo.messages.CREATION_MODE_STARTED
import org.bukkit.entity.Player
import java.util.logging.Logger

class MapCreationCommands(
    val logger: Logger,
    val manager: MapCreationManager,
) {
    val createMap: LiteralCommandNode<CommandSourceStack> =
        literal("mg")
            .then(
                literal("create_map")
                    .then(
                        argument("map_name", StringArgumentType.greedyString())
                            .executes { ctx ->
                                val mapName = StringArgumentType.getString(ctx, "map_name")
                                val playerCreating = ctx.source.sender

                                if (playerCreating !is Player) {
                                    logger.warning("Only players can create maps")
                                    return@executes Command.SINGLE_SUCCESS
                                }

                                manager.createSession(playerCreating, mapName)
                                playerCreating.sendMessage(CREATION_MODE_STARTED)

                                logger.info("Player ${playerCreating.name} started creating map with name $mapName")

                                Command.SINGLE_SUCCESS
                            })
            )
            .then(literal("reload_maps").executes { ctx ->
                manager.mapManager.reloadMaps()

                val playerReloading = ctx.source.sender

                if (playerReloading is Player) {
                    playerReloading.sendMessage("Reloaded maps")
                }

                logger.info("Reloaded maps by command")

                Command.SINGLE_SUCCESS
            })
            .build()
}
