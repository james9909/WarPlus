package com.github.james9909.warplus

import com.github.james9909.warplus.extensions.clearPotionEffects
import com.github.james9909.warplus.extensions.format
import com.github.james9909.warplus.region.Region
import com.github.james9909.warplus.util.DEFAULT_MAX_HEALTH
import com.github.james9909.warplus.util.Message
import com.github.james9909.warplus.util.PlayerState
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.concurrent.ConcurrentHashMap

enum class WarzoneState {
    IDLING,
    RUNNING,
    EDITING
}

class Warzone(
    val plugin: WarPlus,
    val name: String,
    val region: Region,
    val teamSettings: ConfigurationSection = Team.defaultTeamConfiguration(),
    val warzoneSettings: ConfigurationSection = defaultWarzoneConfiguration()
) {
    val enabled = warzoneSettings.getBoolean("enabled", true)
    var state = WarzoneState.IDLING
    val teams = ConcurrentHashMap<TeamKind, Team>()

    fun addTeam(team: Team) = teams.put(team.kind, team)

    fun minPlayers(): Int =
        teams.values.fold(0) { acc, team ->
            acc + team.settings.getInt("min-players", teamSettings.getInt("min-players"))
        }

    fun maxPlayers(): Int =
        teams.values.fold(0) { acc, team ->
            acc + team.settings.getInt("max-players", teamSettings.getInt("max-players"))
        }

    fun contains(location: Location): Boolean = region.contains(location)

    fun numPlayers(): Int = teams.values.fold(0) { acc, team ->
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
        val candidates = teams.values.sortedBy { it.size() }
        for (team in candidates) {
            if (!team.isFull()) {
                return addPlayer(player, team)
            }
        }
        plugin.playerManager.sendMessage(player, Message.TOO_MANY_PLAYERS)
        return false
    }

    @Synchronized
    fun addPlayer(player: Player, team: Team): Boolean {
        assert(!team.isFull())
        team.addPlayer(player)
        plugin.playerManager.savePlayerState(player, team)
        if ("max-health" in warzoneSettings) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = warzoneSettings.getDouble("max-health")
        }
        respawnPlayer(player)

        if (state != WarzoneState.RUNNING && numPlayers() >= minPlayers()) {
            start()
        }

        return true
    }

    fun resetPlayer(player: Player) {
        player.clearPotionEffects()
        val healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)
        val maxHealth = if (healthAttr == null) {
            if ("max-health" in warzoneSettings) {
                warzoneSettings.getDouble("max-health")
            } else {
                DEFAULT_MAX_HEALTH
            }
        } else {
            for (modifier in healthAttr.modifiers) {
                healthAttr.removeModifier(modifier)
            }
            healthAttr.baseValue
        }
        PlayerState(
            health = maxHealth,
            maxHealth = maxHealth,
            inventoryContents = arrayOf()
        ).restore(player)
    }

    fun respawnPlayer(player: Player) {
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
        resetPlayer(player)

        // Pick a random spawn
        val spawn = playerInfo.team.spawns.random()
        player.teleport(spawn.origin)
    }

    fun save() {
        val file = File("${plugin.dataFolder}/warzone-$name.yml")
        val config = YamlConfiguration()

        config.createSection("info")
        config.set("info.world", region.world.name)
        config.set("info.p1", region.p1.format())
        config.set("info.p2", region.p2.format())

        config.set("settings", warzoneSettings)
        config.set("team-settings", teamSettings)
        val teamsSection = config.createSection("teams")
        for ((_, team) in teams) {
            val teamSection = teamsSection.createSection(team.name.toLowerCase())
            team.save(teamSection)
        }
        config.save(file)
    }

    fun unload() {
        for ((_, team) in teams) {
            team.reset()
        }
    }

    companion object {
        fun defaultWarzoneConfiguration(): YamlConfiguration {
            val config = YamlConfiguration()
            config.apply {
                set("enabled", false)
            }
            return config
        }
    }
}
