package com.github.james9909.warplus.structure

import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarPlus
import org.bukkit.Location
import org.bukkit.Material

enum class SpawnStyle {
    INVISIBLE,
    FLAT,
    SMALL,
    LARGE
}

class TeamSpawnStructure(plugin: WarPlus, origin: Location, val kind: TeamKind, style: SpawnStyle) : AbstractStructure(plugin, origin) {
    override val prefix: String = "teams/spawns"
    override fun getStructure(): Array<Array<Array<Material>>> {
        return arrayOf()
    }

    override fun getCorners(): Pair<Location, Location> {
        return Pair(Location(null, 0.0, 0.0, 0.0), Location(null, 0.0, 0.0, 0.0))
    }
}