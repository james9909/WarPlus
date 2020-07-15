package com.github.james9909.warplus.structures

import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.WarTeam
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.extensions.getCardinalDirection
import com.github.james9909.warplus.extensions.getInterCardinalDirection
import com.github.james9909.warplus.util.CardinalDirection
import com.github.james9909.warplus.util.InterCardinalDirection
import com.github.james9909.warplus.util.Orientation
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace

enum class SpawnStyle {
    INVISIBLE,
    FLAT,
    SMALL,
    LARGE
}

private fun getInvisibleCorners(origin: Location): Pair<Location, Location> {
    return Pair(origin, origin.clone().add(0.0, 3.0, 0.0))
}

private fun getInvisibleSpawn(): Array<Array<Array<Material>>> {
    return arrayOf()
}

private fun getSmallCorners(origin: Location): Pair<Location, Location> {
    val orientation = Orientation.fromLocation(origin)
    val topLeft = origin.block
        .getRelative(orientation.front.toBlockFace())
        .getRelative(orientation.left.toBlockFace())
    val bottomRight = origin.block
        .getRelative(orientation.back.toBlockFace())
        .getRelative(orientation.right.toBlockFace())
        .getRelative(BlockFace.UP, 2)
    return Pair(topLeft.location, bottomRight.location)
}

private fun getSmallSpawn(kind: TeamKind): Array<Array<Array<Material>>> {
    val wool = kind.material
    val air = Material.AIR
    val glowstone = Material.GLOWSTONE
    return arrayOf(
        arrayOf(
            arrayOf(wool, wool, wool),
            arrayOf(wool, glowstone, wool),
            arrayOf(wool, wool, wool)
        ),
        arrayOf(
            arrayOf(air, air, air),
            arrayOf(air, air, air),
            arrayOf(air, air, air)
        ),
        arrayOf(
            arrayOf(air, air, air),
            arrayOf(air, air, air),
            arrayOf(air, air, air)
        )
    )
}

private fun getFlatCorners(origin: Location): Pair<Location, Location> {
    val orientation = Orientation.fromLocation(origin)
    val topLeft = origin.block
        .getRelative(orientation.front.toBlockFace(), 2)
        .getRelative(orientation.left.toBlockFace(), 2)
    val bottomRight = origin.block
        .getRelative(orientation.back.toBlockFace(), 2)
        .getRelative(orientation.right.toBlockFace(), 2)
        .getRelative(BlockFace.UP, 3)
    return Pair(topLeft.location, bottomRight.location)
}

private fun getFlatSpawn(kind: TeamKind): Array<Array<Array<Material>>> {
    val wool = kind.material
    val air = Material.AIR
    val glowstone = Material.GLOWSTONE
    return arrayOf(
        arrayOf(
            arrayOf(wool, wool, wool, wool, wool),
            arrayOf(wool, wool, wool, wool, wool),
            arrayOf(wool, wool, glowstone, wool, wool),
            arrayOf(wool, wool, wool, wool, wool),
            arrayOf(wool, wool, wool, wool, wool)
        ),
        arrayOf(
            arrayOf(air, air, air, air, air),
            arrayOf(air, air, air, air, air),
            arrayOf(air, air, air, air, air),
            arrayOf(air, air, air, air, air),
            arrayOf(air, air, air, air, air)
        ),
        arrayOf(
            arrayOf(air, air, air, air, air),
            arrayOf(air, air, air, air, air),
            arrayOf(air, air, air, air, air),
            arrayOf(air, air, air, air, air),
            arrayOf(air, air, air, air, air)
        )
    )
}

private fun getLargeCorners(origin: Location): Pair<Location, Location> {
    val orientation = Orientation.fromLocation(origin)
    val topLeft = origin.block
        .getRelative(orientation.front.toBlockFace(), 2)
        .getRelative(orientation.left.toBlockFace(), 2)
    val bottomRight = origin.block
        .getRelative(orientation.back.toBlockFace(), 2)
        .getRelative(orientation.right.toBlockFace(), 2)
        .getRelative(BlockFace.UP, 4)
    return Pair(topLeft.location, bottomRight.location)
}

private fun getLargeSpawn(kind: TeamKind, origin: Location): Array<Array<Array<Material>>> {
    val interCardinal = origin.getInterCardinalDirection()
    val cardinal = origin.getCardinalDirection()
    val wool = kind.material
    val glowstone = Material.GLOWSTONE
    val air = Material.AIR
    return if (
        (cardinal == CardinalDirection.NORTH && interCardinal == InterCardinalDirection.NORTH_EAST) ||
        (cardinal == CardinalDirection.EAST && interCardinal == InterCardinalDirection.SOUTH_EAST) ||
        (cardinal == CardinalDirection.SOUTH && interCardinal == InterCardinalDirection.SOUTH_WEST) ||
        (cardinal == CardinalDirection.WEST && interCardinal == InterCardinalDirection.NORTH_WEST)
    ) {
        // Open to the right
        arrayOf(
            arrayOf(
                arrayOf(wool, wool, wool, wool, wool),
                arrayOf(wool, wool, wool, wool, wool),
                arrayOf(wool, wool, glowstone, wool, wool),
                arrayOf(wool, wool, wool, wool, wool),
                arrayOf(wool, wool, wool, wool, wool)
            ),
            arrayOf(
                arrayOf(wool, wool, air, air, air),
                arrayOf(wool, air, air, air, air),
                arrayOf(wool, air, air, air, air),
                arrayOf(wool, air, air, air, wool),
                arrayOf(wool, wool, wool, wool, wool)
            ),
            arrayOf(
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, air),
                arrayOf(wool, air, air, air, air),
                arrayOf(wool, air, air, air, air),
                arrayOf(wool, wool, wool, air, air)
            ),
            arrayOf(
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, air),
                arrayOf(wool, air, air, air, air),
                arrayOf(wool, wool, air, air, air)
            ),
            arrayOf(
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, air),
                arrayOf(wool, air, air, air, air)
            )
        )
    } else {
        // Open to the left
        arrayOf(
            arrayOf(
                arrayOf(wool, wool, wool, wool, wool),
                arrayOf(wool, wool, wool, wool, wool),
                arrayOf(wool, wool, glowstone, wool, wool),
                arrayOf(wool, wool, wool, wool, wool),
                arrayOf(wool, wool, wool, wool, wool)
            ),
            arrayOf(
                arrayOf(air, air, air, wool, wool),
                arrayOf(air, air, air, air, wool),
                arrayOf(air, air, air, air, wool),
                arrayOf(wool, air, air, air, wool),
                arrayOf(wool, wool, wool, wool, wool)
            ),
            arrayOf(
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, wool),
                arrayOf(air, air, air, air, wool),
                arrayOf(air, air, wool, wool, wool)
            ),
            arrayOf(
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, wool),
                arrayOf(air, air, air, wool, wool)
            ),
            arrayOf(
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, wool)
            )
        )
    }
}

class TeamSpawnStructure(plugin: WarPlus, origin: Location, val kind: TeamKind, private val style: SpawnStyle) :
    AbstractStructure(plugin, origin) {
    override val prefix: String = "teams/spawns"
    override val corners by lazy {
        when (style) {
            SpawnStyle.INVISIBLE -> getInvisibleCorners(origin)
            SpawnStyle.FLAT -> getFlatCorners(origin)
            SpawnStyle.SMALL -> getSmallCorners(origin)
            SpawnStyle.LARGE -> getLargeCorners(origin)
        }
    }
    private val signBlock by lazy {
        when (style) {
            SpawnStyle.INVISIBLE -> null
            SpawnStyle.FLAT -> origin.block
                .getRelative(origin.getInterCardinalDirection().toBlockFace(), 2)
            SpawnStyle.SMALL -> origin.block
                .getRelative(origin.getInterCardinalDirection().toBlockFace(), 1)
            SpawnStyle.LARGE -> origin.block
                .getRelative(origin.getInterCardinalDirection().toBlockFace(), 2)
        }?.getRelative(BlockFace.UP, 1)
    }
    private val interCardinalDirection = origin.getInterCardinalDirection()

    override fun getStructure(): Array<Array<Array<Material>>> {
        return when (style) {
            SpawnStyle.INVISIBLE -> getInvisibleSpawn()
            SpawnStyle.FLAT -> getFlatSpawn(kind)
            SpawnStyle.SMALL -> getSmallSpawn(kind)
            SpawnStyle.LARGE -> getLargeSpawn(kind, origin)
        }
    }

    override fun postBuild() {
        signBlock?.apply {
            type = Material.SIGN
            val signData = blockData as org.bukkit.block.data.type.Sign
            signData.rotation = interCardinalDirection.toBlockFace().oppositeFace
            blockData = signData
        }
    }

    fun updateSign(team: WarTeam) {
        signBlock?.apply {
            val sign = state as org.bukkit.block.Sign
            sign.setLine(0, "Team ${kind.name.toLowerCase()}")
            sign.setLine(1, "${team.size()}/${team.maxPlayers()} players")
            sign.setLine(2, "${team.score}/${team.maxScore()} points")
            when (val lives = team.lives) {
                1 -> sign.setLine(3, "1 life left")
                else -> sign.setLine(3, "$lives lives left")
            }
            sign.update(true)
        }
    }
}