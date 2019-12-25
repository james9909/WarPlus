package com.github.james9909.warplus.extensions

import com.github.james9909.warplus.util.CardinalDirection
import org.bukkit.Location
import kotlin.math.roundToInt

val axis = arrayOf(CardinalDirection.NORTH, CardinalDirection.EAST, CardinalDirection.SOUTH, CardinalDirection.WEST)

fun Location.format(): String {
    // Format: world:x,y,z[,[yaw],[pitch]]
    return "${this.world?.name}:${this.x},${this.y},${this.z},${this.yaw},${this.pitch}"
}

fun Location.getCardinalDirection(): CardinalDirection {
    return axis[(yaw / 90F).roundToInt() and 0x3]
}