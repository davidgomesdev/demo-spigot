package me.davidgomes.demo.heroes

import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.*

private const val SENDER_TAG = "sender"

fun setEntitySender(
    plugin: Plugin,
    entity: Entity,
    sender: Player,
) {
    entity.persistentDataContainer.set(
        NamespacedKey(plugin, SENDER_TAG),
        PersistentDataType.STRING,
        sender.uniqueId.toString(),
    )
}

fun getSenderOf(
    plugin: Plugin,
    entity: Entity,
): Player? {
    val senderId =
        entity.persistentDataContainer.get(
            NamespacedKey(plugin, SENDER_TAG),
            PersistentDataType.STRING,
        ) ?: return null
    val sender = plugin.server.getPlayer(UUID.fromString(senderId))

    if (sender == null) {
        plugin.logger.warning("Could not find sender of entity '${entity.name}' with id '$senderId', maybe they left the server?")
        return null
    }

    return sender
}
