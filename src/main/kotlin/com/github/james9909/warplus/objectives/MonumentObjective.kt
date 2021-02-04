package com.github.james9909.warplus.objectives

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.config.WarzoneConfigType
import com.github.james9909.warplus.extensions.format
import com.github.james9909.warplus.extensions.toLocation
import com.github.james9909.warplus.runnable.MonumentRunnable
import com.github.james9909.warplus.structures.MonumentStructure
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

fun createMonumentObjective(plugin: WarPlus, warzone: Warzone, config: ConfigurationSection): MonumentObjective {
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
) : Objective(plugin, warzone) {
    override val name = "monuments"

    private var timer = MonumentRunnable(plugin, warzone)

    fun getMonumentAtLocation(location: Location): MonumentStructure? = monuments.firstOrNull { it.contains(location) }

    fun addMonument(monument: MonumentStructure) = monuments.add(monument)

    fun removeMonument(monument: MonumentStructure): Boolean = monuments.remove(monument)

    override fun handleBlockBreak(player: Player?, block: Block): Boolean {
        val monument = monuments.firstOrNull { it.contains(block.location) } ?: return false
        if (player == null) return true
        val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId) ?: return true
        if (playerInfo.team.warzone != warzone) return true
        if (playerInfo.team.kind == monument.owner || block.location != monument.blockLocation) return true
        if (warzone.teams.count { it.value.hasEnoughPlayers() } <= 1) return true

        warzone.broadcast("Team ${monument.owner?.format()} has lost monument ${monument.name}!")
        monument.owner = null
        return false
    }

    override fun handleBlockPlace(entity: Entity?, block: Block): Boolean {
        val monument = monuments.firstOrNull { it.contains(block.location) } ?: return false
        if (block.location != monument.blockLocation) return true
        if (entity !is Player) return true
        val playerInfo = plugin.playerManager.getPlayerInfo(entity.uniqueId) ?: return true
        if (playerInfo.team.warzone != warzone || block.type != playerInfo.team.kind.material) return true
        if (warzone.teams.count { it.value.hasEnoughPlayers() } <= 1) return true

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

    override fun reset() = monuments.forEach {
        it.build()
        it.owner = null
    }

    override fun start() {
        try {
            timer.runTaskTimer(plugin, 0, warzone.warzoneSettings.get(WarzoneConfigType.MONUMENT_TIMER_INTERVAL).toLong())
        } catch (e: IllegalStateException) {
            // Ignore the exception, this isn't harmful.
        }
    }

    override fun stop() {
        try {
            timer.cancel()
            timer = MonumentRunnable(plugin, warzone)
        } catch (e: IllegalStateException) {
            // Ignore the exception, this isn't harmful.
        }
    }

    override fun delete() = monuments.forEach { it.deleteVolume() }
}
