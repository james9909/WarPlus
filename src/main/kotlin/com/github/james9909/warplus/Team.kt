package com.github.james9909.warplus

import com.github.james9909.warplus.extensions.format
import com.github.james9909.warplus.structure.FlagStructure
import com.github.james9909.warplus.structure.TeamSpawnStructure
import com.google.common.collect.ImmutableList
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player

enum class TeamKind(val material: Material, val chatColor: ChatColor) {
    WHITE(Material.WHITE_WOOL, ChatColor.WHITE),
    ORANGE(Material.ORANGE_WOOL, ChatColor.GOLD),
    MAGENTA(Material.MAGENTA_WOOL, ChatColor.LIGHT_PURPLE),
    BLUE(Material.LIGHT_BLUE_WOOL, ChatColor.BLUE),
    GOLD(Material.YELLOW_WOOL, ChatColor.YELLOW),
    GREEN(Material.GREEN_WOOL, ChatColor.GREEN),
    PINK(Material.PINK_WOOL, ChatColor.LIGHT_PURPLE),
    GRAY(Material.GRAY_WOOL, ChatColor.DARK_GRAY),
    IRON(Material.LIGHT_GRAY_WOOL, ChatColor.GRAY),
    DIAMOND(Material.CYAN_WOOL, ChatColor.DARK_AQUA),
    PURPLE(Material.PURPLE_WOOL, ChatColor.DARK_PURPLE),
    NAVY(Material.BLUE_WOOL, ChatColor.DARK_BLUE),
    BROWN(Material.BROWN_WOOL, ChatColor.DARK_RED),
    DARKGREEN(Material.GREEN_WOOL, ChatColor.DARK_GREEN),
    RED(Material.RED_WOOL, ChatColor.RED),
    BLACK(Material.BLACK_WOOL, ChatColor.BLACK);
}

class Team(
    val kind: TeamKind,
    val spawns: MutableList<TeamSpawnStructure>,
    val warzone: Warzone,
    val settings: ConfigurationSection = defaultTeamConfiguration()
) {
    val players = mutableSetOf<Player>()
    val flagStructures = mutableListOf<FlagStructure>()

    fun addFlag(flagStructure: FlagStructure) = flagStructures.add(flagStructure)

    fun addPlayer(player: Player) = players.add(player)

    fun removePlayer(player: Player) = players.remove(player)

    fun size(): Int = players.size

    fun isFull(): Boolean = size() == settings.getInt("max-players", 5)

    fun reset() {
        for (player in ImmutableList.copyOf(players)) {
            removePlayer(player)
        }
        for (flagStructure in flagStructures) {
            flagStructure.build()
        }
        for (spawn in spawns) {
            spawn.build()
        }
    }

    fun addTeamSpawn(spawn: TeamSpawnStructure) {
        spawns.add(spawn)
    }

    fun removeTeamSpawn(spawn: TeamSpawnStructure) {
        spawns.remove(spawn)
    }

    fun save(teamSection: ConfigurationSection) {
        teamSection.set("settings", settings)
        val spawnsStringList = mutableListOf<String>()
        for (spawn in spawns) {
            spawnsStringList.add(spawn.origin.format())
        }
        teamSection.set("spawns", spawnsStringList)

        val flagsStringList = mutableListOf<String>()
        for (flag in flagStructures) {
            flagsStringList.add(flag.origin.format())
        }
        teamSection.set("flags", flagsStringList)
    }

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