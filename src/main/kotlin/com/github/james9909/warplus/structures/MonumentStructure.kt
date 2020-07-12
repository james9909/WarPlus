package com.github.james9909.warplus.structures

import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.util.Orientation
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace

class MonumentStructure(plugin: WarPlus, origin: Location, val name: String) : AbstractStructure(plugin, origin) {
    override val prefix = "zone/monuments"
    override val corners by lazy {
        val topLeft = origin.block
            .getRelative(orientation.front.toBlockFace(), 2)
            .getRelative(orientation.left.toBlockFace(), 2)
        val bottomRight = origin.block
            .getRelative(orientation.back.toBlockFace(), 2)
            .getRelative(orientation.right.toBlockFace(), 2)
            .getRelative(BlockFace.UP, 3)
        Pair(topLeft.location, bottomRight.location)
    }
    val blockLocation by lazy {
        origin.block.getRelative(BlockFace.UP, 2).location
    }
    var owner: TeamKind? = null

    override fun getStructure(): Array<Array<Array<Material>>> {
        val obsidian = Material.OBSIDIAN
        val glowstone = Material.GLOWSTONE
        val air = Material.AIR
        return arrayOf(
            arrayOf(
                arrayOf(glowstone, obsidian, obsidian, obsidian, glowstone),
                arrayOf(obsidian, obsidian, obsidian, obsidian, obsidian),
                arrayOf(obsidian, obsidian, obsidian, obsidian, obsidian),
                arrayOf(obsidian, obsidian, obsidian, obsidian, obsidian),
                arrayOf(glowstone, obsidian, obsidian, obsidian, glowstone)
            ),
            arrayOf(
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, air),
                arrayOf(air, obsidian, obsidian, obsidian, air),
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, air)
            ),
            arrayOf(
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, air),
                arrayOf(air, obsidian, air, obsidian, air),
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, air)
            ),
            arrayOf(
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, air),
                arrayOf(air, obsidian, obsidian, obsidian, air),
                arrayOf(air, air, air, air, air),
                arrayOf(air, air, air, air, air)
            )
        )
    }
}