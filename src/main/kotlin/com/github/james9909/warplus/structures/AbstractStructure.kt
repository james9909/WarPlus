package com.github.james9909.warplus.structures

import com.github.james9909.warplus.WarError
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.WorldEditError
import com.github.james9909.warplus.region.Region
import com.github.james9909.warplus.util.Orientation
import com.github.james9909.warplus.util.copyRegion
import com.github.james9909.warplus.util.loadSchematic
import com.github.james9909.warplus.util.pasteSchematic
import com.github.james9909.warplus.util.saveClipboard
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import java.io.File

abstract class AbstractStructure(val plugin: WarPlus, val origin: Location) {
    val region by lazy {
        val (p1, p2) = corners
        Region(
            origin.world ?: plugin.server.worlds[0],
            p1,
            p2
        )
    }
    val orientation = Orientation.fromLocation(origin)

    abstract val prefix: String
    abstract val corners: Pair<Location, Location>
    abstract fun getStructure(): Array<Array<Array<Material>>>

    private fun getFolder(): String {
        return "${plugin.dataFolder.absolutePath}/volumes/$prefix"
    }

    private fun getVolumePath(): String {
        return "${getFolder()}/${origin.blockX}-${origin.blockY}-${origin.blockZ}.schem"
    }

    fun contains(location: Location): Boolean {
        return region.contains(location)
    }

    fun saveVolume(): Result<Unit, WarError> {
        if (plugin.server.pluginManager.getPlugin("WorldEdit") == null) {
            return Err(WorldEditError("WorldEdit is not loaded"))
        }
        val (pos1, pos2) = corners
        val region = CuboidRegion(
            BukkitAdapter.adapt(pos1.world ?: plugin.server.worlds[0]),
            BlockVector3.at(pos1.blockX, pos1.blockY, pos1.blockZ),
            BlockVector3.at(pos2.blockX, pos2.blockY, pos2.blockZ)
        )
        val clipboard = copyRegion(region)
        if (clipboard is Err) {
            return clipboard
        }
        saveClipboard(clipboard.unwrap(), getVolumePath())
        return Ok(Unit)
    }

    fun restoreVolume(): Result<Unit, WarError> {
        if (plugin.server.pluginManager.getPlugin("WorldEdit") == null) {
            return Err(WorldEditError("WorldEdit is not loaded"))
        }

        val clipboard = loadSchematic(getVolumePath())
        if (clipboard is Err) {
            return clipboard
        }
        val (x, y, z) = region.getMinimumPoint()
        val to = Location(origin.world, x.toDouble(), y.toDouble(), z.toDouble())
        pasteSchematic(clipboard.unwrap(), to, false)
        return Ok(Unit)
    }

    fun deleteVolume(): Boolean {
        val file = File(getVolumePath())
        if (file.exists()) {
            return file.delete()
        }
        return false
    }

    fun build() {
        val structure = getStructure()
        val (topLeft, _) = corners
        structure.forEachIndexed { yOffset, layer ->
            val blockY = topLeft.block.getRelative(BlockFace.UP, yOffset)
            layer.forEachIndexed { zOffset, row ->
                val blockYZ = blockY.getRelative(orientation.back.toBlockFace(), zOffset)
                row.forEachIndexed { xOffset, material ->
                    val blockXYZ = blockYZ.getRelative(orientation.right.toBlockFace(), xOffset)
                    blockXYZ.type = material
                }
            }
        }
        postBuild()
    }

    open fun postBuild() {}
}
