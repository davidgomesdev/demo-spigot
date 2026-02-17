package me.davidgomes.demo.pdc

import me.davidgomes.demo.plugin
import org.bukkit.Location
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import kotlin.io.encoding.Base64

object LocationDataType : PersistentDataType<ByteArray, Location> {
    override fun getPrimitiveType(): Class<ByteArray> = ByteArray::class.java

    override fun getComplexType(): Class<Location> = Location::class.java

    override fun toPrimitive(
        complex: Location,
        context: PersistentDataAdapterContext,
    ): ByteArray {
        val encodedWorldName = Base64.encode(requireNotNull(complex.world).name.toByteArray())

        return "$encodedWorldName,${complex.x},${complex.y},${complex.z},${complex.yaw},${complex.pitch}".toByteArray()
    }

    override fun fromPrimitive(
        primitive: ByteArray,
        context: PersistentDataAdapterContext,
    ): Location {
        val data = String(primitive).split(",")
        val decodedWorldName = Base64.decode(data[0]).toString()
        val x = data[1].toDouble()
        val y = data[2].toDouble()
        val z = data[3].toDouble()
        val yaw = data[4].toFloat()
        val pitch = data[5].toFloat()

        return Location(plugin.server.getWorld(decodedWorldName), x, y, z, yaw, pitch)
    }
}
