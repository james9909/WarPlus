package com.github.james9909.warplus.config

import org.bukkit.configuration.ConfigurationSection

typealias ConfigType<T> = (ConfigurationSection, String, T) -> T

private val BOOLEAN: ConfigType<Boolean> = { config, path, default ->
    config.getBoolean(path, default)
}

private val INTEGER: ConfigType<Int> = { config, path, default ->
    config.getInt(path, default)
}

private val STRING: ConfigType<String> = { config, path, default ->
    config.getString(path) ?: default
}

private val DOUBLE: ConfigType<Double> = { config, path, default ->
    config.getDouble(path, default)
}

private val STRING_LIST: ConfigType<List<String>> = { config, path, default ->
    val list = config.getStringList(path)
    if (list.isEmpty()) default else list
}

fun booleanKey(path: String, default: Boolean): ConfigKey<Boolean> = ConfigKey(BOOLEAN, path, default)

fun stringKey(path: String, default: String): ConfigKey<String> = ConfigKey(STRING, path, default)

fun integerKey(path: String, default: Int): ConfigKey<Int> = ConfigKey(INTEGER, path, default)

fun doubleKey(path: String, default: Double): ConfigKey<Double> = ConfigKey(DOUBLE, path, default)

fun stringListKey(path: String, default: List<String>): ConfigKey<List<String>> = ConfigKey(STRING_LIST, path, default)

inline fun <reified E : Enum<E>> enumKey(path: String, default: E): ConfigKey<E> {
    return ConfigKey({ config, _, _ ->
        enumValueOf(config.getString(path)?.uppercase() ?: default.toString())
    }, path, default)
}
