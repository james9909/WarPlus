package com.github.james9909.warplus.config

import org.bukkit.configuration.ConfigurationSection

class ConfigKey<T>(val type: ConfigType<T>, val path: String, val default: T) {
    fun get(config: ConfigurationSection): T {
        return type(config, path, default)
    }

    fun get(config: ConfigurationSection, newDefault: T): T {
        return type(config, path, newDefault)
    }
}
