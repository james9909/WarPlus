package com.github.james9909.warplus.util

import com.boydti.fawe.util.EditSessionBuilder
import com.github.james9909.warplus.FileError
import com.github.james9909.warplus.InvalidSchematicError
import com.github.james9909.warplus.WarError
import com.github.james9909.warplus.WorldEditError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.WorldEditException
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.bukkit.BukkitWorld
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.session.ClipboardHolder
import org.bukkit.Location
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

fun loadSchematic(fileName: String): Result<Clipboard, WarError> {
    val file = File(fileName)
    if (!file.exists()) {
        return Err(FileError("File '$fileName' does not exist"))
    }
    val format =
        ClipboardFormats.findByFile(file) ?: return Err(InvalidSchematicError("No schematic detected: $fileName"))
    return try {
        val reader = format.getReader(FileInputStream(file))
        Ok(reader.read())
    } catch (e: IOException) {
        e.printStackTrace()
        Err(FileError(e.message ?: "IOException"))
    }
}

fun pasteSchematic(clipboard: Clipboard, location: Location, ignoreAirBlocks: Boolean): Result<Unit, WarError> {
    try {
        val session = EditSessionBuilder(BukkitWorld(location.world))
            .autoQueue(true)
            .fastmode(true)
            .combineStages(true)
            .checkMemory(false)
            .limitUnlimited()
            .build()
        session.use {
            val clipboardHolder = ClipboardHolder(clipboard)
            val operation = clipboardHolder
                .createPaste(session)
                .to(BlockVector3.at(location.blockX, location.blockY, location.blockZ))
                .ignoreAirBlocks(ignoreAirBlocks)
                .build()
            Operations.complete(operation)
        }
    } catch (e: WorldEditException) {
        e.printStackTrace()
        return Err(WorldEditError(e.message ?: "WorldEditException"))
    }
    return Ok(Unit)
}

fun copyRegion(cuboidRegion: CuboidRegion): Result<Clipboard, WarError> {
    val clipboard = BlockArrayClipboard(cuboidRegion)
    val editSession = WorldEdit.getInstance().editSessionFactory.getEditSession(cuboidRegion.world, -1)
    try {
        editSession.use {
            val forwardExtentCopy = ForwardExtentCopy(editSession, cuboidRegion, clipboard, cuboidRegion.minimumPoint)
            forwardExtentCopy.isCopyingEntities = false
            Operations.complete(forwardExtentCopy)
        }
    } catch (e: WorldEditException) {
        e.printStackTrace()
        return Err(WorldEditError(e.message ?: "WorldEditException"))
    }
    return Ok(clipboard)
}

fun saveClipboard(clipboard: Clipboard, fileName: String): Result<Unit, WarError> {
    val file = File(fileName)
    if (!file.parentFile.exists()) {
        file.parentFile.mkdirs()
    }
    try {
        val fileOutputStream = FileOutputStream(file)
        fileOutputStream.use {
            val writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(FileOutputStream(file))
            writer.use {
                writer.write(clipboard)
            }
        }
    } catch (e: WorldEditException) {
        e.printStackTrace()
        return Err(WorldEditError(e.message ?: "WorldEditException"))
    } catch (e: IOException) {
        e.printStackTrace()
        return Err(FileError(e.message ?: "IOException"))
    }
    return Ok(Unit)
}
