package me.davidgomes.demo

import org.bukkit.ChatColor

operator fun ChatColor.plus(other: String): String = toString()+other
