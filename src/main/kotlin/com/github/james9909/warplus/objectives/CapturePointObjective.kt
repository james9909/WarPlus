package com.github.james9909.warplus.objectives

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.extensions.format
import com.github.james9909.warplus.extensions.toLocation
import com.github.james9909.warplus.runnable.CapturePointRunnable
import com.github.james9909.warplus.structures.CapturePointStructure
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

const val CAPTURE_POINT_TIMER_INTERVAL_TICKS = 20L

fun createCapturePointObjective(plugin: WarPlus, warzone: Warzone, config: ConfigurationSection): CapturePointObjective? {
    val capturePoints = mutableListOf<CapturePointStructure>()
    config.getMapList("locations").forEach { cpMap ->
        val name = cpMap["name"] as String
        val origin = (cpMap["origin"] as String).toLocation()
        capturePoints.add(CapturePointStructure(plugin, origin, name))
    }
    return CapturePointObjective(
        plugin, warzone, capturePoints
    )
}

class CapturePointObjective(
    private val plugin: WarPlus,
    private val warzone: Warzone,
    val capturePoints: MutableList<CapturePointStructure>
) : Objective(plugin, warzone) {
    override val name = "capture_points"
    var timer = CapturePointRunnable(plugin, warzone)

    fun getCapturePointAtLocation(location: Location): CapturePointStructure? = capturePoints.firstOrNull { it.contains(location) }

    fun addCapturePoint(cp: CapturePointStructure) = capturePoints.add(cp)

    fun removeCapturePoint(cp: CapturePointStructure): Boolean = capturePoints.remove(cp)

    override fun handleBlockBreak(player: Player?, block: Block): Boolean {
        return capturePoints.firstOrNull { it.contains(block.location) } != null
    }

    override fun handleBlockPlace(entity: Entity, block: Block): Boolean {
        return capturePoints.firstOrNull { it.contains(block.location) } != null
    }

    override fun saveConfig(config: ConfigurationSection) {
        config.set("locations", capturePoints.map {
            mapOf(
                "name" to it.name,
                "origin" to it.origin.format()
            )
        })
    }

    override fun reset() {
        capturePoints.forEach {
            it.reset()
            it.build()
        }
    }

    override fun start() {
        timer.runTaskTimer(plugin, 0, CAPTURE_POINT_TIMER_INTERVAL_TICKS)
    }

    override fun stop() {
        timer.cancel()
        timer = CapturePointRunnable(plugin, warzone)
    }

    override fun delete() {
        capturePoints.forEach { it.deleteVolume() }
    }
}
