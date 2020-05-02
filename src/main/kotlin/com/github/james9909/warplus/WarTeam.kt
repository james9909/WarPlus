package com.github.james9909.warplus

import com.github.james9909.warplus.config.CascadingConfig
import com.github.james9909.warplus.config.TeamConfigType
import com.github.james9909.warplus.extensions.format
import com.github.james9909.warplus.structures.TeamSpawnStructure
import com.github.james9909.warplus.util.WarScoreboard
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

enum class TeamKind(val material: Material, val chatColor: ChatColor) {
    BLACK(Material.BLACK_WOOL, ChatColor.BLACK),
    BLUE(Material.LIGHT_BLUE_WOOL, ChatColor.BLUE),
    BROWN(Material.BROWN_WOOL, ChatColor.DARK_RED),
    DARKGREEN(Material.GREEN_WOOL, ChatColor.DARK_GREEN),
    DIAMOND(Material.CYAN_WOOL, ChatColor.DARK_AQUA),
    GOLD(Material.YELLOW_WOOL, ChatColor.YELLOW),
    GRAY(Material.GRAY_WOOL, ChatColor.DARK_GRAY),
    GREEN(Material.GREEN_WOOL, ChatColor.GREEN),
    IRON(Material.LIGHT_GRAY_WOOL, ChatColor.GRAY),
    MAGENTA(Material.MAGENTA_WOOL, ChatColor.LIGHT_PURPLE),
    NAVY(Material.BLUE_WOOL, ChatColor.DARK_BLUE),
    ORANGE(Material.ORANGE_WOOL, ChatColor.GOLD),
    PINK(Material.PINK_WOOL, ChatColor.LIGHT_PURPLE),
    PURPLE(Material.PURPLE_WOOL, ChatColor.DARK_PURPLE),
    RED(Material.RED_WOOL, ChatColor.RED),
    WHITE(Material.WHITE_WOOL, ChatColor.WHITE);

    fun format(): String {
        return "${chatColor}${name.toLowerCase()}${ChatColor.RESET}"
    }
}

class WarTeam(
    val kind: TeamKind,
    val spawns: MutableList<TeamSpawnStructure>,
    val warzone: Warzone,
    val settings: CascadingConfig = CascadingConfig()
) {
    val players = mutableSetOf<Player>()
    var lives = maxLives()
    var score = 0
        private set

    fun addPlayer(player: Player) {
        players.add(player)
        WarScoreboard.createScoreboard(player, warzone)
        warzone.teams.forEach { (_, team) ->
            team.players.forEach {
                WarScoreboard.getScoreboard(it)?.addPlayer(this, player)
            }
        }
    }

    fun removePlayer(player: Player) {
        players.remove(player)
        WarScoreboard.removeScoreboard(player)
        warzone.teams.forEach { (_, team) ->
            team.players.forEach {
                WarScoreboard.getScoreboard(it)?.removePlayer(this, player)
            }
        }
    }

    fun size(): Int = players.size

    fun hasEnoughPlayers(): Boolean = size() >= settings.get(TeamConfigType.MIN_PLAYERS)

    fun isFull(): Boolean = size() >= settings.get(TeamConfigType.MAX_PLAYERS)

    fun maxScore(): Int = settings.get(TeamConfigType.MAX_SCORE)

    fun maxLives(): Int = settings.get(TeamConfigType.LIVES)

    fun resetAttributes() {
        lives = maxLives()
        score = 0
    }

    fun reset() {
        players.clear()
        resetSpawns()
    }

    fun resetSpawns() {
        spawns.forEach {
            it.build()
        }
    }

    fun addTeamSpawn(spawn: TeamSpawnStructure) {
        spawns.add(spawn)
    }

    fun removeTeamSpawn(spawn: TeamSpawnStructure) {
        spawns.remove(spawn)
    }

    fun saveConfig(teamSection: ConfigurationSection) {
        teamSection.set("settings", settings.config)
        val spawnsStringList = mutableListOf<String>()
        for (spawn in spawns) {
            spawnsStringList.add(spawn.origin.format())
        }
        teamSection.set("spawns", spawnsStringList)
    }

    fun broadcast(message: String) {
        players.forEach {
            warzone.plugin.playerManager.sendMessage(it, message)
        }
    }

    @Synchronized
    fun addPoint() {
        score += 1
    }

    fun getScoreboardName(): String = "${warzone.name}_${kind.name.toLowerCase()}"

    override fun toString(): String {
        return kind.format()
    }
}