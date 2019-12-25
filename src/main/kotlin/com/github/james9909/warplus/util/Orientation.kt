package com.github.james9909.warplus.util

import com.github.james9909.warplus.extensions.getCardinalDirection
import org.bukkit.Location
import org.bukkit.block.BlockFace

enum class CardinalDirection {
    NORTH,
    SOUTH,
    EAST,
    WEST;

    fun toBlockFace(): BlockFace {
        return when (this) {
            NORTH -> BlockFace.NORTH
            SOUTH -> BlockFace.SOUTH
            EAST -> BlockFace.EAST
            WEST -> BlockFace.WEST
        }
    }
}

data class Orientation(val left: CardinalDirection, val right: CardinalDirection, val front: CardinalDirection, val back: CardinalDirection) {

    companion object {
        fun fromLocation(location: Location): Orientation {
            return when (location.getCardinalDirection()) {
                CardinalDirection.NORTH -> Orientation(
                    CardinalDirection.WEST,
                    CardinalDirection.EAST,
                    CardinalDirection.NORTH,
                    CardinalDirection.SOUTH
                )
                CardinalDirection.EAST -> Orientation(
                    CardinalDirection.NORTH,
                    CardinalDirection.SOUTH,
                    CardinalDirection.EAST,
                    CardinalDirection.WEST
                )
                CardinalDirection.SOUTH -> Orientation(
                    CardinalDirection.EAST,
                    CardinalDirection.WEST,
                    CardinalDirection.SOUTH,
                    CardinalDirection.NORTH
                )
                CardinalDirection.WEST -> Orientation(
                    CardinalDirection.SOUTH,
                    CardinalDirection.NORTH,
                    CardinalDirection.WEST,
                    CardinalDirection.EAST
                )
            }
        }
    }
}
