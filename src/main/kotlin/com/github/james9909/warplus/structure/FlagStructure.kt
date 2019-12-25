package com.github.james9909.warplus.structure

import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.util.Orientation
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace

class FlagStructure(plugin: WarPlus, origin: Location, private val kind: TeamKind) :
    AbstractStructure(plugin, origin) {
    override val prefix = "teams/flags"

    override fun getStructure(): Array<Array<Array<Material>>> {
        val obsidian = Material.OBSIDIAN
        val glowstone = Material.GLOWSTONE
        val air = Material.AIR
        val fence = Material.OAK_FENCE
        return arrayOf(
            arrayOf(
                arrayOf(obsidian, obsidian, obsidian),
                arrayOf(obsidian, glowstone, obsidian),
                arrayOf(obsidian, obsidian, obsidian)
            ),
            arrayOf(
                arrayOf(air, air, air),
                arrayOf(air, air, air),
                arrayOf(air, fence, air)
            ),
            arrayOf(
                arrayOf(air, air, air),
                arrayOf(air, kind.material, air),
                arrayOf(air, fence, air)
            ),
            arrayOf(
                arrayOf(air, air, air),
                arrayOf(air, fence, air),
                arrayOf(air, fence, air)
            )
        )
    }

    override fun getCorners(): Pair<Location, Location> {
        val orientation = Orientation.fromLocation(origin)
        val topLeft = origin.block
            .getRelative(orientation.front.toBlockFace())
            .getRelative(orientation.left.toBlockFace())
        val bottomRight = origin.block
            .getRelative(orientation.back.toBlockFace())
            .getRelative(orientation.right.toBlockFace())
            .getRelative(BlockFace.UP, 3)
        return Pair(topLeft.location, bottomRight.location)
    }
}