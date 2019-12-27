package com.github.james9909.warplus

import com.github.james9909.warplus.extensions.clearPotionEffects
import com.github.james9909.warplus.extensions.format
import com.github.james9909.warplus.region.Region
import com.github.james9909.warplus.util.DEFAULT_MAX_HEALTH
import com.github.james9909.warplus.util.Message
import com.github.james9909.warplus.util.PlayerState
import com.github.james9909.warplus.util.copyRegion
import com.github.james9909.warplus.util.loadSchematic
import com.github.james9909.warplus.util.pasteSchematic
import com.github.james9909.warplus.util.saveClipboard
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.Result
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
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
    val volumeFolder = "${plugin.dataFolder.absolutePath}/volumes/warzones"
    val volumePath = "$volumeFolder/$name.schem"

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

    fun saveConfig() {
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
            val teamSection = teamsSection.createSection(team.kind.name.toLowerCase())
            team.saveConfig(teamSection)
        }
        config.save(file)
    }

    fun unload() {
        for ((_, team) in teams) {
            team.reset()
        }
    }

    fun saveVolume(): Result<Unit, WarError> {
        val (minX, minY, minZ) = region.getMinimumPoint()
        val (maxX, maxY, maxZ) = region.getMaximumPoint()
        val region = CuboidRegion(
            BukkitAdapter.adapt(region.world),
            BlockVector3.at(minX, minY, minZ),
            BlockVector3.at(maxX, maxY, maxZ)
        )
        val clipboard = copyRegion(region)
        if (clipboard is Err) {
            return clipboard
        }
        saveClipboard(clipboard.unwrap(), volumePath)
        return Ok(Unit)
    }

    fun restoreVolume(): Result<Unit, WarError> {
        val clipboard = loadSchematic(volumePath)
        if (clipboard is Err) {
            return clipboard
        }
        val (x, y, z) = region.getMinimumPoint()
        val to = Location(region.world, x.toDouble(), y.toDouble(), z.toDouble())
        pasteSchematic(clipboard.unwrap(), to, false)
        return Ok(Unit)
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
