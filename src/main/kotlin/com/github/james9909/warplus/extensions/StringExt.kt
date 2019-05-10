@file:JvmName("StringUtils")

package com.github.james9909.warplus.extensions

import org.bukkit.Bukkit
import org.bukkit.Location
import java.lang.NumberFormatException

class LocationFormatException(message: String) : IllegalArgumentException(message)

fun String.toLocation(): Location {
    // Format: world:x,y,z[,[yaw],[pitch]]
    val split = this.split(":")
    if (split.size != 2) {
        throw LocationFormatException("Invalid location string: $this")
    }

    val world = Bukkit.getWorld(split[0]) ?: throw LocationFormatException("Invalid world: ${split[0]}")
    val coords = split[1].split(",").toTypedArray().copyOf(5)

    try {
        val x = coords[0]?.toDouble() ?: 0.0
        val y = coords[1]?.toDouble() ?: 0.0
        val z = coords[2]?.toDouble() ?: 0.0
        val yaw = coords[3]?.toFloat() ?: 0F
        val pitch = coords[4]?.toFloat() ?: 0F
        return Location(world, x, y, z, yaw, pitch)
    } catch (e: NumberFormatException) {
        throw LocationFormatException("Invalid coordinates: $coords")
    }
}
