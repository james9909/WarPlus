package com.github.james9909.warplus.region

import org.bukkit.Location
import org.bukkit.World

data class Region(
    val world: World,
    var p1: Location = Location(world, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY),
    var p2: Location = Location(world, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
) {

    fun contains(location: Location): Boolean {
        if (world.name != location.world?.name) {
            return false
        }

        return contains(location.blockX, location.blockY, location.blockZ)
    }

    fun contains(x: Int, y: Int, z: Int): Boolean = (x >= p1.blockX && x <= p2.blockX) &&
        (y >= p1.blockY && y <= p2.blockY) &&
        (z >= p1.blockZ && y <= p2.blockZ)
}