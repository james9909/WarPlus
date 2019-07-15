@file:JvmName("ConfigUtils")

package com.github.james9909.warplus.extensions

import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection

fun ConfigurationSection.getOrCreateSection(path: String): ConfigurationSection {
    return getConfigurationSection(path) ?: createSection(path)
}

fun ConfigurationSection.getLocation(path: String): Location? {
    if (!isString(path)) {
        return null
    }

    return getString(path)?.toLocation()
}

fun ConfigurationSection.getLocationList(path: String): List<Location> {
    val locations = mutableListOf<Location>()
    getStringList(path).forEach {
        locations.add(it.toLocation())
    }
    return locations
}

fun ConfigurationSection.merge(other: ConfigurationSection) {
    for (key in other.getKeys(false)) {
        if (!contains(key)) {
            set(key, other.get(key))
        }
    }
}