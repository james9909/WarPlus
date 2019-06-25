package com.github.james9909.warplus

import com.github.james9909.warplus.extensions.clearPotionEffects
import com.github.james9909.warplus.region.Region
import com.github.james9909.warplus.util.Message
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

enum class WarzoneState {
    IDLING,
    RUNNING,
    EDITING
}

class Warzone(
    val plugin: WarPlus,
    val name: String,
    val region: Region,
    val teamSettings: ConfigurationSection,
    val warzoneSettings: ConfigurationSection
) {
    val enabled = warzoneSettings.getBoolean("enabled", true)
    var state = WarzoneState.IDLING
    val teams = mutableListOf<Team>()

    fun addTeam(team: Team) = teams.add(team)

    fun minPlayers(): Int =
        teams.fold(0) { acc, team ->
            acc + team.settings.getInt("min-players", teamSettings.getInt("min-players"))
        }

    fun maxPlayers(): Int =
        teams.fold(0) { acc, team ->
            acc + team.settings.getInt("max-players", teamSettings.getInt("max-players"))
        }

    fun contains(location: Location): Boolean = region.contains(location)

    fun numPlayers(): Int = teams.fold(0) { acc, team ->
        acc + team.size()
    }

    fun start() {
        state = WarzoneState.RUNNING
    }

    @Synchronized
    fun removePlayer(player: Player, team: Team) {
        team.removePlayer(player)
        plugin.playerManager.restorePlayerState(player)
    }

    @Synchronized
    fun addPlayer(player: Player): Boolean {
        if (numPlayers() == maxPlayers()) {
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
        plugin.playerManager.savePlayerState(player, toJoin)
        if ("max-health" in warzoneSettings) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = warzoneSettings.getDouble("max-health")
        }
        respawn(player)

        if (state != WarzoneState.RUNNING && numPlayers() >= minPlayers()) {
            start()
        }

        return true
    }

    fun respawn(player: Player) {
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return

        // Reset the player
        player.apply {
            inventory.clear()
            clearPotionEffects()

            // Restore health
            val healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)
            health = if (healthAttr == null) {
                DEFAULT_MAX_HEALTH
            } else {
                for (modifier in healthAttr.modifiers) {
                    healthAttr.removeModifier(modifier)
                }
                healthAttr.baseValue
            }
            remainingAir = maximumAir
            foodLevel = MAX_FOOD_LEVEL
            saturation = DEFAULT_SATURATION
            exhaustion = 0F
            fallDistance = 0F
            level = 0
            exp = 0F
            isFlying = false
            allowFlight = false
            fireTicks = 0
            gameMode = GameMode.SURVIVAL
        }

        // Pick a random spawn
        val spawn = playerInfo.team?.spawns?.random() ?: return
        player.teleport(spawn)
    }
}
