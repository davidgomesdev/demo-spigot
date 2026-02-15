package me.davidgomes.demo.heroes.butcher

import me.davidgomes.demo.heroes.Hero

object ButcherHero : Hero(
    "Butcher",
    listOf(
        ButcherItems.meatCleaver,
        ButcherItems.anvilDropItem
    )
)