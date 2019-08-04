package com.github.james9909.warplus.structure

import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarPlus
import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.function.pattern.BlockPattern
import com.sk89q.worldedit.world.block.BlockTypes
import org.bukkit.Location

enum class SpawnStyle {
    INVISIBLE,
    FLAT,
    SMALL,
    LARGE
}

class TeamSpawnStructure(plugin: WarPlus, origin: Location, val kind: TeamKind, style: SpawnStyle) : AbstractStructure(plugin, origin) {
    override val schematicFile = "spawn-${style.name.toLowerCase()}.schem"

    override fun beforePaste(editSession: EditSession) {
        val from = mutableSetOf(BlockTypes.WHITE_WOOL?.defaultState?.toBaseBlock())
        val to = BlockPattern(BukkitAdapter.asBlockType(kind.material).defaultState)
        editSession.replaceBlocks(schematic.region, from, to)
    }

    override fun afterPaste() {}
}