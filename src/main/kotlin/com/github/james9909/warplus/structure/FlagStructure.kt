package com.github.james9909.warplus.structure

import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarPlus
import com.sk89q.worldedit.EditSession
import org.bukkit.Location

class FlagStructure(plugin: WarPlus, origin: Location, val kind: TeamKind) : AbstractStructure(plugin, origin) {
    override val schematicFile = "flag.schem"
    private val flagBlock: Location by lazy {
        origin.add(0.0, 2.0, 0.0)
    }

    override fun beforePaste(editSession: EditSession) {}

    override fun afterPaste() {
        val flagBlock = flagBlock
        val blockState = flagBlock.block.state
        blockState.type = kind.material
    }
}