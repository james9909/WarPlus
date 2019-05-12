package com.github.james9909.warplus

import com.github.james9909.warplus.region.Region
import com.github.james9909.warplus.util.Message
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

const val DEFAULT_MIN_PLAYERS = 2
const val DEFAULT_MAX_PLAYERS = 20

class Warzone(
    val plugin: WarPlus,
    val name: String,
    val world: World,
    val region: Region,
    val teamSettings: ConfigurationSection,
    val warzoneSettings: ConfigurationSection
) {
    val enabled: Boolean = warzoneSettings.getBoolean("enabled", true)
    var running: Boolean = false
    val teams = mutableListOf<Team>()

    fun addTeam(team: Team) = teams.add(team)

    fun contains(location: Location): Boolean = region.contains(location)

    fun numPlayers(): Int = teams.fold(0) { acc, team ->
        acc + team.size()
    }

    fun start() {
        if (!enabled || running) {
            return
        }
        if (numPlayers() < warzoneSettings.getInt("min-players", DEFAULT_MIN_PLAYERS)) {
            return
        }

        running = true
    }

    fun join(player: Player): Boolean {
        if (numPlayers() == warzoneSettings.getInt("max-players", DEFAULT_MAX_PLAYERS)) {
            plugin.playerManager.sendMessage(player, Message.TOO_MANY_PLAYERS)
            return false
        }

        // Find candidate team to join
        var toJoin: Team? = null
        teams.sortBy { it.size() }
        for (team in teams) {
            if (team.isFull()) {
                continue
            }
            toJoin = team
            break
        }

        if (toJoin == null) {
            plugin.playerManager.sendMessage(player, Message.TOO_MANY_PLAYERS)
            return false
        }

        toJoin.addPlayer(player)

        return true
    }

    fun respawn(player: Player) {
    }
}
