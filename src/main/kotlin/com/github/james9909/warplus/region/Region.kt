package com.github.james9909.warplus.region

import org.bukkit.Location
import org.bukkit.World
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

    fun overlapsWith(other: Region): Boolean {
        val minPoint = getMinimumPoint()
        val maxPoint = getMaximumPoint()
        val otherMinPoint = other.getMinimumPoint()
        val otherMaxPoint = other.getMaximumPoint()
        return minPoint.first <= otherMaxPoint.first &&
            maxPoint.first >= otherMinPoint.first &&
            minPoint.second <= otherMaxPoint.second &&
            maxPoint.second >= otherMinPoint.second &&
            minPoint.third <= otherMaxPoint.third &&
            maxPoint.third >= otherMinPoint.third
    }

    private fun getMaxX(): Int {
        return max(p1.blockX, p2.blockX)
    }

    private fun getMaxY(): Int {
        return max(p1.blockY, p2.blockY)
    }

    private fun getMaxZ(): Int {
        return max(p1.blockZ, p2.blockZ)
    }

    private fun getMinX(): Int {
        return min(p1.blockX, p2.blockX)
    }

    private fun getMinY(): Int {
        return min(p1.blockY, p2.blockY)
    }

    private fun getMinZ(): Int {
        return min(p1.blockZ, p2.blockZ)
    }

    fun getMinimumPoint(): Triple<Int, Int, Int> {
        return Triple(getMinX(), getMinY(), getMinZ())
    }

    fun getMaximumPoint(): Triple<Int, Int, Int> {
        return Triple(getMaxX(), getMaxY(), getMaxZ())
    }
}