package com.github.james9909.warplus

import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

class Team(
    val name: String,
    val spawns: List<Location>,
    val warzone: com.github.james9909.warplus.Warzone,
    val settings: ConfigurationSection
) {
    private val players = mutableSetOf<Player>()

    fun addPlayer(player: Player) = players.add(player)

    fun size(): Int = players.size

    fun isFull(): Boolean = size() == settings.getInt("max-players", 5)
}