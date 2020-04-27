package com.github.james9909.warplus.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration

data class CascadingConfig(val config: ConfigurationSection, private val nested: CascadingConfig?) {
    constructor() : this(YamlConfiguration(), null)
    constructor(config: ConfigurationSection) : this(config, null)

    fun <T> get(key: ConfigKey<T>): T {
        if (config.contains(key.path)) {
            return key.get(config)
        }
        if (nested != null) {
            return nested.get(key)
        }
        return key.default
    }

    fun <T> get(key: ConfigKey<T>, default: T): T {
        if (config.contains(key.path)) {
            return key.get(config, default)
        }
        if (nested != null) {
            return nested.get(key, default)
        }
        return default
    }

    fun <T> put(key: ConfigKey<T>, value: T) {
        config[key.path] = value
    }
}