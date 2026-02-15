package me.davidgomes.demo.messages

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

const val NOT_ENOUGH_PLAYERS_TO_START = "Sorry... But playing alone is no fun! Wait for more players to join the arena before starting it!"
const val NOT_IN_ARENA = "You are NOT in the arena!"
const val ALREADY_IN_ARENA = "You are already in the arena!"
const val JOINED_ARENA = "You have joined the arena!"
const val ARENA_STARTED = "Arena has started!"
val YOU_LOST = Component.text("You lost! Better luck next time...", NamedTextColor.GRAY)
val YOU_WON = Component.text("Congratulations! You won the arena match!", NamedTextColor.YELLOW)
