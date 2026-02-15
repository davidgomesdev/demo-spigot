package utils

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ExYamlConfiguration(
    val file: File,
) : YamlConfiguration() {
    init {
        reload()
    }

    fun reload() =
        this.apply {
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }

            load(file)
        }

    fun setAndSave(
        path: String,
        value: Any?,
    ): ExYamlConfiguration =
        this.apply {
            set(path, value)
            save(file)
        }
}
