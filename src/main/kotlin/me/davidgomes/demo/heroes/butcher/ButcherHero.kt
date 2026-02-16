package me.davidgomes.demo.heroes.butcher

import me.davidgomes.demo.heroes.Hero

object ButcherHero : Hero(
    "Butcher",
    ButcherItems.selectorItem,
    listOf(
        ButcherItems.meatCleaver,
        ButcherItems.anvilDropItem
    )
)