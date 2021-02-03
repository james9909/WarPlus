package com.github.james9909.warplus.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration

data class CascadingConfig(val config: ConfigurationSection, private val nested: CascadingConfig?) {
    constructor() : this(YamlConfiguration(), null)
    constructor(config: ConfigurationSection) : this(config, null)

    fun <T> get(key: ConfigKey<T>): T =
        when {
            config.contains(key.path) -> key.get(config)
            nested != null -> nested.get(key)
            else -> key.default
        }

    fun <T> get(key: ConfigKey<T>, default: T): T =
        when {
            config.contains(key.path) -> key.get(config, default)
            nested != null -> nested.get(key, default)
            else -> default
        }

    fun <T> put(key: ConfigKey<T>, value: T) {
        config[key.path] = value
    }
}
