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
import java.lang.IllegalStateException

const val CAPTURE_POINT_TIMER_INTERVAL_TICKS = 20L

fun createCapturePointObjective(plugin: WarPlus, warzone: Warzone, config: ConfigurationSection): CapturePointObjective {
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
    private var timer = CapturePointRunnable(plugin, warzone)

    fun getCapturePointAtLocation(location: Location): CapturePointStructure? = capturePoints.firstOrNull { it.contains(location) }

    fun addCapturePoint(cp: CapturePointStructure) = capturePoints.add(cp)

    fun removeCapturePoint(cp: CapturePointStructure): Boolean = capturePoints.remove(cp)

    override fun handleBlockBreak(player: Player?, block: Block): Boolean {
        return capturePoints.firstOrNull { it.contains(block.location) } != null
    }

    override fun handleBlockPlace(entity: Entity?, block: Block): Boolean {
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
        try {
            timer.runTaskTimer(plugin, 0, CAPTURE_POINT_TIMER_INTERVAL_TICKS)
        } catch (e: IllegalStateException) {
            // Ignore the exception, this isn't harmful.
        }
    }

    override fun stop() {
        try {
            timer.cancel()
            timer = CapturePointRunnable(plugin, warzone)
        } catch (e: IllegalStateException) {
            // Ignore the exception, this isn't harmful.
        }
    }

    override fun delete() {
        capturePoints.forEach { it.deleteVolume() }
    }
}
