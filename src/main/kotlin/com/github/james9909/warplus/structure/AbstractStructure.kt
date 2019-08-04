package com.github.james9909.warplus.structure

import com.github.james9909.warplus.WarPlus
import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import org.bukkit.Location
import java.io.File
import java.io.FileInputStream

abstract class AbstractStructure(val plugin: WarPlus, val origin: Location) {
    abstract val schematicFile: String
    protected val schematic by lazy {
        loadSchematic("${plugin.dataFolder}/schematics", schematicFile)
    }
    private val original by lazy {
        // loadSchematic("${plugin.dataFolder}/volumes", "$name-original.schem")
    }

    fun reset() {
        val editSession =
            WorldEdit.getInstance().editSessionFactory.getEditSession(BukkitAdapter.adapt(origin.world), -1)
        beforePaste(editSession)
        val operation = ClipboardHolder(schematic)
            .createPaste(editSession)
            .to(BlockVector3.at(origin.blockX, origin.blockY + 1, origin.blockZ))
            .build()
        Operations.complete(operation)
        afterPaste()
        editSession.flushSession()
    }

    abstract fun beforePaste(editSession: EditSession)

    abstract fun afterPaste()

    /*
    fun restore() {
        val editSession =
            WorldEdit.getInstance().editSessionFactory.getEditSession(BukkitAdapter.adapt(origin.world), -1)
        val operation = ClipboardHolder(original)
            .createPaste(editSession)
            .to(BlockVector3.at(origin.blockX, origin.blockY + 1, origin.blockZ))
            .build()
        Operations.complete(operation)
        editSession.flushSession()
    }
    */

    fun contains(location: Location): Boolean {
        return origin == location
    }

    fun loadSchematic(folderName: String, fileName: String): Clipboard {
        val folder = File(folderName)
        if (!folder.exists()) folder.mkdirs()
        val file = File(folder, fileName)
        val format = ClipboardFormats.findByFile(file)!!
        val reader = format.getReader(FileInputStream(file))
        return reader.read()
    }
}