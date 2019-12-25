package com.github.james9909.warplus.extensions

import com.github.james9909.warplus.util.CardinalDirection
import com.github.james9909.warplus.util.InterCardinalDirection
import org.bukkit.Location
import kotlin.math.roundToInt

val axis = arrayOf(CardinalDirection.SOUTH, CardinalDirection.WEST, CardinalDirection.NORTH, CardinalDirection.EAST)
val radial = arrayOf(InterCardinalDirection.SOUTH_WEST, InterCardinalDirection.NORTH_WEST, InterCardinalDirection.NORTH_EAST, InterCardinalDirection.SOUTH_EAST)

fun Location.format(): String {
    // Format: world:x,y,z[,[yaw],[pitch]]
    return "${this.world?.name}:${this.x},${this.y},${this.z},${this.yaw},${this.pitch}"
}

fun Location.getCardinalDirection(): CardinalDirection {
    return axis[(yaw / 90F).roundToInt() and 0x3]
}

fun Location.getInterCardinalDirection(): InterCardinalDirection {
    val adjustedYaw = if (yaw < 0) {
        (yaw + 360) % 360
    } else {
        yaw
    }
    return radial[(adjustedYaw / 90).toInt()]
}

fun Location.blockLocation(): Location {
    return Location(world, blockX.toDouble(), blockY.toDouble(), blockZ.toDouble(), yaw, pitch)
}
