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
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.util.Vector
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
    private val volumeFolder = "${plugin.dataFolder.absolutePath}/volumes/warzones"
    private val volumePath = "$volumeFolder/$name.schem"

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

    private fun canStart(): Boolean {
        val sufficientTeams = teams.values.map { team ->
            team.hasEnoughPlayers()
        }.filter { v ->
            v
        }.size
        return sufficientTeams >= warzoneSettings.getInt("min-teams", 2)
    }

    private fun start() {
        plugin.logger.info("Starting warzone $name")
        state = WarzoneState.RUNNING
    }

    @Synchronized
    fun removePlayer(player: Player, team: Team) {
        team.removePlayer(player)
        plugin.playerManager.restorePlayerState(player)
    }

    @Synchronized
    fun addPlayer(player: Player): Boolean {
        if (numPlayers() >= maxPlayers()) {
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
    private fun addPlayer(player: Player, team: Team): Boolean {
        assert(!team.isFull())
        team.addPlayer(player)
        plugin.playerManager.savePlayerState(player, team)
        if ("max-health" in warzoneSettings) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = warzoneSettings.getDouble("max-health")
        }
        respawnPlayer(player)
        broadcast("${player.name} joined team $team")

        if (state != WarzoneState.RUNNING && canStart()) {
            start()
        }
        return true
    }

    private fun resetPlayer(player: Player) {
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

    private fun respawnPlayer(player: Player) {
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
        resetPlayer(player)
        Bukkit.getScheduler().runTaskLater(plugin, { -> player.velocity = Vector() }, 1)

        // Pick a random spawn
        val spawn = playerInfo.team.spawns.random()
        val spawnLocation = spawn.origin

        // Offset because the origin is in the ground
        spawnLocation.add(0.0, 1.0, 0.0)
        // We want players to be looking straight ahead
        spawnLocation.pitch = 0F

        player.teleport(spawnLocation)
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

    private fun handleDeath(player: Player) {
        respawnPlayer(player)
    }

    fun broadcast(message: String) {
        teams.values.forEach {
            it.broadcast(message)
        }
    }

    fun handleSuicide(player: Player) {
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
        if (warzoneSettings.getBoolean("death-messages")) {
            broadcast("${playerInfo.team.kind.chatColor}${player.name}${ChatColor.RESET} committed suicide")
        }
        handleDeath(player)
    }

    fun handleKill(attacker: Player, defender: Player, damager: Entity, direct: Boolean) {
        val attackerInfo = plugin.playerManager.getPlayerInfo(attacker) ?: return
        val defenderInfo = plugin.playerManager.getPlayerInfo(defender) ?: return

        if (warzoneSettings.getBoolean("death-messages")) {
            val weapon = attacker.inventory.itemInMainHand
            val weaponName = if (weapon.hasItemMeta() && weapon.itemMeta!!.hasDisplayName()) {
                weapon.itemMeta!!.displayName
            } else if (weapon.type == Material.AIR) {
                "air"
            } else {
                weapon.type.toString()
            }
            val attackerColor = attackerInfo.team.kind.chatColor
            val defenderColor = defenderInfo.team.kind.chatColor
            val message = "${attackerColor}${attacker.name}${ChatColor.RESET}'s $weaponName killed ${defenderColor}${defender.name}${ChatColor.RESET}"
            broadcast(message)
        }

        handleDeath(defender)
    }

    fun handleNaturalDeath(player: Player, cause: DamageCause) {
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
        val playerString = "${playerInfo.team.kind.chatColor}${player.name}${ChatColor.RESET}"
        val message = when (cause) {
            DamageCause.BLOCK_EXPLOSION -> "$playerString exploded"
            DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.LAVA, DamageCause.LIGHTNING -> "$playerString burned to a crisp"
            DamageCause.DROWNING -> "$playerString drowned"
            DamageCause.FALL -> "$playerString fell to an untimely death"
            DamageCause.SUFFOCATION -> "$playerString suffocated"
            else -> {
                plugin.logger.info("Unhandled cause of death: $cause")
                "$playerString died"
            }
        }
        broadcast(message)
        handleDeath(player)
    }

    fun handleMobDeath(player: Player, entity: LivingEntity) {
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
        val playerString = "${playerInfo.team.kind.chatColor}${player.name}${ChatColor.RESET}"
        broadcast("$playerString was slain by ${entity.name}")
        handleDeath(player)
    }

    companion object {
        private fun defaultWarzoneConfiguration(): YamlConfiguration {
            val config = YamlConfiguration()
            config.apply {
                set("enabled", false)
            }
            return config
        }
    }
}
