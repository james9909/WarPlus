package com.github.james9909.warplus.objectives

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.extensions.format
import com.github.james9909.warplus.extensions.toLocation
import com.github.james9909.warplus.structures.MonumentStructure
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection

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

    fun getMonumentAtLocation(location: Location): MonumentStructure? = monuments.firstOrNull { it.contains(location)
    }

    fun addMonument(monument: MonumentStructure) = monuments.add(monument)

    fun removeMonument(monument: MonumentStructure): Boolean = monuments.remove(monument)

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
        }
    }
}