package com.github.james9909.warplus.region

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import kotlin.math.max
import kotlin.math.min

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

    fun contains(x: Int, y: Int, z: Int): Boolean = (x >= getMinX() && x <= getMaxX()) &&
        (y >= getMinY() && y <= getMaxY()) &&
        (z >= getMinZ() && z <= getMaxZ())

    fun getMaxX(): Int {
        return max(p1.blockX, p2.blockX)
    }

    fun getMaxY(): Int {
        return max(p1.blockY, p2.blockY)
    }

    fun getMaxZ(): Int {
        return max(p1.blockZ, p2.blockZ)
    }

    fun getMinX(): Int {
        return min(p1.blockX, p2.blockX)
    }

    fun getMinY(): Int {
        return min(p1.blockY, p2.blockY)
    }

    fun getMinZ(): Int {
        return min(p1.blockZ, p2.blockZ)
    }
}