package com.github.james9909.warplus.objectives

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.extensions.format
import com.github.james9909.warplus.extensions.toLocation
import com.github.james9909.warplus.structures.CapturePointStructure
import org.bukkit.configuration.ConfigurationSection

fun createCapturePointObjective(plugin: WarPlus, warzone: Warzone, config: ConfigurationSection): CapturePointObjective? {
    val capturePoints = mutableListOf<CapturePointStructure>()
    config.getMapList("locations").forEach { monumentMap ->
        val name = monumentMap["name"] as String
        val origin = (monumentMap["origin"] as String).toLocation()
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
            it.build()
        }
    }

    override fun delete() {
        capturePoints.forEach { it.deleteVolume() }
    }
}
