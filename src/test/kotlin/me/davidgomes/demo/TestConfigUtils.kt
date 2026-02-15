package me.davidgomes.demo

import utils.ExYamlConfiguration
import java.io.File

fun createTempConfig() = ExYamlConfiguration(loadTempFile())

fun loadTempFile(): File =
    File.createTempFile("test", ".yml").apply {
        deleteOnExit()
    }
