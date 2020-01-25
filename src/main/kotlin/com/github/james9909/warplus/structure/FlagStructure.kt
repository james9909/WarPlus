package com.github.james9909.warplus.structure

import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.util.Orientation
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace

class FlagStructure(plugin: WarPlus, origin: Location, val kind: TeamKind) :
    AbstractStructure(plugin, origin) {
    override val prefix = "teams/flags"
    override val corners: Pair<Location, Location> by lazy {
        val orientation = Orientation.fromLocation(origin)
        val topLeft = origin.block
            .getRelative(orientation.front.toBlockFace())
            .getRelative(orientation.left.toBlockFace())
        val bottomRight = origin.block
            .getRelative(orientation.back.toBlockFace())
            .getRelative(orientation.right.toBlockFace())
            .getRelative(BlockFace.UP, 3)
        Pair(topLeft.location, bottomRight.location)
    }
    val flagBlock: Block by lazy {
        origin.block.getRelative(BlockFace.UP, 2)
    }

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
}