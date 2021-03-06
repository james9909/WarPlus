package com.github.james9909.warplus.structures

import com.github.james9909.warplus.WarPlus
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace

class BombStructure(plugin: WarPlus, origin: Location, val name: String) : AbstractStructure(plugin, origin) {
    override val prefix = "zone/bombs"
    override val corners by lazy {
        val topLeft = origin.block
            .getRelative(orientation.front.toBlockFace())
            .getRelative(orientation.left.toBlockFace())
        val bottomRight = origin.block
            .getRelative(orientation.back.toBlockFace())
            .getRelative(orientation.right.toBlockFace())
            .getRelative(BlockFace.UP, 3)
        Pair(topLeft.location, bottomRight.location)
    }
    val tntBlock by lazy {
        origin.block.getRelative(BlockFace.UP, 2)
    }

    override fun getStructure(): Array<Array<Array<Material>>> {
        val obsidian = Material.OBSIDIAN
        val glowstone = Material.GLOWSTONE
        val air = Material.AIR
        val fence = Material.OAK_FENCE
        val tnt = Material.TNT
        return arrayOf(
            arrayOf(
                arrayOf(obsidian, obsidian, obsidian),
                arrayOf(obsidian, glowstone, obsidian),
                arrayOf(obsidian, obsidian, obsidian)
            ),
            arrayOf(
                arrayOf(air, air, air),
                arrayOf(air, fence, air),
                arrayOf(air, air, air)
            ),
            arrayOf(
                arrayOf(air, air, air),
                arrayOf(air, tnt, air),
                arrayOf(air, air, air)
            ),
            arrayOf(
                arrayOf(air, air, air),
                arrayOf(air, air, air),
                arrayOf(air, air, air)
            )
        )
    }
}
