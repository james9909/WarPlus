package com.github.james9909.warplus.structures

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.util.Orientation
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Directional

class WarzonePortalStructure(plugin: WarPlus, origin: Location, val name: String, val warzone: Warzone) : AbstractStructure(plugin, origin) {
    override val prefix = "zone/portals"
    // Origin starts at the player's legs, not beneath it
    override val corners by lazy {
        val orientation = Orientation.fromLocation(origin)
        val topLeft = origin.block
            .getRelative(orientation.left.toBlockFace())
        val bottomRight = origin.block
            .getRelative(orientation.right.toBlockFace())
            .getRelative(BlockFace.UP, 2)
        Pair(topLeft.location, bottomRight.location)
    }
    val signBlock by lazy {
        val orientation = Orientation.fromLocation(origin)
        origin.block
            .getRelative(orientation.back.toBlockFace())
            .getRelative(BlockFace.UP, 2)
    }

    override fun getStructure(): Array<Array<Array<Material>>> {
        return emptyArray()
    }

    override fun postBuild() {
        val orientation = Orientation.fromLocation(origin)
        signBlock.type = Material.WALL_SIGN
        val signData = signBlock.blockData as Directional
        signData.facing = orientation.back.toBlockFace()
        signBlock.blockData = signData
        updateSign()
    }

    fun updateSign() {
        val block = signBlock.state
        val sign = block as org.bukkit.block.Sign
        sign.setLine(0, "Warzone")
        sign.setLine(1, warzone.name)
        sign.setLine(2, "${warzone.numPlayers()}/${warzone.maxPlayers()}")
        sign.setLine(3, "${warzone.teams.size} teams")
        block.update(true)
    }
}