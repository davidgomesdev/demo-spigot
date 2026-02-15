package utils

import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

fun PlayerInteractEvent.isNotLeftClick(): Boolean =
    !(action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)

fun PlayerInteractEvent.isNotRightClick(): Boolean =
    !(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
