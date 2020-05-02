package com.github.james9909.warplus.objectives

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.extensions.format
import com.github.james9909.warplus.extensions.toLocation
import com.github.james9909.warplus.structures.MonumentStructure
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

fun createMonumentObjective(plugin: WarPlus, warzone: Warzone, config: ConfigurationSection): MonumentObjective? {
    val monuments: MutableList<MonumentStructure> = mutableListOf()
    config.getMapList("locations").forEach { monumentMap ->
        val name = monumentMap["name"] as String
        val origin = (monumentMap["origin"] as String).toLocation()
        monuments.add(MonumentStructure(plugin, origin, name))
    }
    return MonumentObjective(
        plugin, warzone, monuments
    )
}

class MonumentObjective(
    private val plugin: WarPlus,
    private val warzone: Warzone,
    val monuments: MutableList<MonumentStructure>
) : AbstractObjective(plugin, warzone) {
    override val name = "monuments"

    fun getMonumentAtLocation(location: Location): MonumentStructure? = monuments.firstOrNull { it.contains(location) }

    fun addMonument(monument: MonumentStructure) = monuments.add(monument)

    fun removeMonument(monument: MonumentStructure): Boolean = monuments.remove(monument)

    override fun handleBlockBreak(player: Player?, block: Block): Boolean {
        val monument = monuments.firstOrNull { it.contains(block.location) } ?: return false
        if (player == null) {
            return true
        }
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return true
        if (playerInfo.team.warzone != warzone) {
            return true
        }
        if (playerInfo.team.kind == monument.owner || block.location != monument.blockLocation) {
            return true
        }
        warzone.broadcast("Team ${monument.owner?.format()} has lost monument ${monument.name}!")
        monument.owner = null
        return false
    }

    override fun handleBlockPlace(player: Player, block: Block): Boolean {
        val monument = monuments.firstOrNull { it.contains(block.location) } ?: return false
        if (block.location != monument.blockLocation) {
            // Can only place into the center block
            return true
        }
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return true
        if (playerInfo.team.warzone != warzone || block.type != playerInfo.team.kind.material) {
            return true
        }
        warzone.broadcast("Team ${playerInfo.team} has captured monument ${monument.name}!")
        monument.owner = playerInfo.team.kind
        return false
    }

    override fun saveConfig(config: ConfigurationSection) {
        config.set("locations", monuments.map {
            mapOf(
                "name" to it.name,
                "origin" to it.origin.format()
            )
        })
    }

    override fun reset() {
        monuments.forEach {
            it.build()
            it.owner = null
        }
    }
}