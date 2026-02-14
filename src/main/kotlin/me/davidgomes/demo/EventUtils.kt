package me.davidgomes.demo

import org.bukkit.event.player.PlayerInteractEvent


fun PlayerInteractEvent.isLeftClick(): Boolean {
    return action == org.bukkit.event.block.Action.LEFT_CLICK_AIR || action == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK
}

fun PlayerInteractEvent.isRightClick(): Boolean {
    return action == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
}
