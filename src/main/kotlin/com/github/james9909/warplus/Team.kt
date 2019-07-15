package com.github.james9909.warplus

import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.yaml.snakeyaml.Yaml

class Team(
    val name: String,
    val spawns: MutableList<Location>,
    val warzone: Warzone,
    val settings: ConfigurationSection = defaultTeamConfiguration()
) {
    private val players = mutableSetOf<Player>()

    fun addPlayer(player: Player) = players.add(player)

    fun removePlayer(player: Player) = players.remove(player)

    fun size(): Int = players.size

    fun isFull(): Boolean = size() == settings.getInt("max-players", 5)

    companion object {
        fun defaultTeamConfiguration(): YamlConfiguration {
            val config = YamlConfiguration()
            config.apply {
                set("lives", 20)
                set("min-players", 1)
                set("max-players", 20)
            }
            return config
        }
    }
}