@file:JvmName("StringUtils")

package com.github.james9909.warplus.extensions

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class LocationFormatException(message: String) : IllegalArgumentException(message)

fun String.toLocation(): Location {
    // Format: world:x,y,z[,[yaw],[pitch]]
    val split = this.split(":")
    if (split.size != 2) throw LocationFormatException("Invalid location string: $this")

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

fun String.toPotionEffect(): PotionEffect? {
    if (isEmpty()) return null
    val parts = split(":")
    val effect = PotionEffectType.getByName(parts[0]) ?: return null
    return when (parts.size) {
        1 -> {
            PotionEffect(effect, 20, 1)
        }
        2 -> {
            PotionEffect(effect, 20, parts[1].toInt())
        }
        else -> {
            PotionEffect(effect, parts[2].toInt(), parts[1].toInt())
        }
    }
}

fun String.color(): String = ChatColor.translateAlternateColorCodes('&', this)
