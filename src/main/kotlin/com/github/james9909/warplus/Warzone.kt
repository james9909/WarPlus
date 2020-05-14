package com.github.james9909.warplus

import com.github.james9909.warplus.config.CascadingConfig
import com.github.james9909.warplus.config.TeamConfigType
import com.github.james9909.warplus.config.WarzoneConfigType
import com.github.james9909.warplus.extensions.clearPotionEffects
import com.github.james9909.warplus.extensions.format
import com.github.james9909.warplus.objectives.AbstractObjective
import com.github.james9909.warplus.objectives.FlagObjective
import com.github.james9909.warplus.objectives.MonumentObjective
import com.github.james9909.warplus.region.Region
import com.github.james9909.warplus.structures.FlagStructure
import com.github.james9909.warplus.structures.MonumentStructure
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
import com.google.common.collect.ImmutableList
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.util.Vector
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

enum class WarzoneState {
    IDLING,
    RUNNING,
    EDITING
}

class Warzone(
    val plugin: WarPlus,
    val name: String,
    val region: Region,
    val teamSettings: CascadingConfig = CascadingConfig(),
    val warzoneSettings: CascadingConfig = CascadingConfig(),
    val objectives: HashMap<String, AbstractObjective> = hashMapOf()
) {
    var state = WarzoneState.IDLING
    val teams = ConcurrentHashMap<TeamKind, WarTeam>()
    private val volumeFolder = "${plugin.dataFolder.absolutePath}/volumes/warzones"
    private val volumePath = "$volumeFolder/$name.schem"

    fun isEnabled(): Boolean = warzoneSettings.get(WarzoneConfigType.ENABLED)

    fun addTeam(team: WarTeam) = teams.put(team.kind, team)

    fun minPlayers(): Int = teams.values.sumBy { it.settings.get(TeamConfigType.MIN_PLAYERS) }

    fun maxPlayers(): Int = teams.values.sumBy { it.settings.get(TeamConfigType.MAX_PLAYERS) }

    fun contains(location: Location): Boolean = region.contains(location)

    fun numPlayers(): Int = teams.values.sumBy { it.size() }

    private fun canStart(): Boolean {
        val sufficientTeams = teams.values.map { team ->
            team.hasEnoughPlayers()
        }.filter { v ->
            v
        }.size
        return sufficientTeams >= warzoneSettings.get(WarzoneConfigType.MIN_TEAMS)
    }

    private fun start() {
        plugin.logger.info("Starting warzone $name")
        state = WarzoneState.RUNNING

        initialize()
    }

    private fun initialize() {
        for ((_, team) in teams) {
            team.resetAttributes()
            team.resetSpawns()
            for (player in team.players) {
                respawnPlayer(player)
            }
        }
        if (warzoneSettings.get(WarzoneConfigType.REMOVE_ENTITIES_ON_RESET)) {
            removeEntities()
        }
        resetObjectives()
    }

    private fun reinitialize() {
        for ((_, team) in teams) {
            team.resetSpawns()
            for (player in team.players) {
                respawnPlayer(player)
            }
        }
        if (warzoneSettings.get(WarzoneConfigType.REMOVE_ENTITIES_ON_RESET)) {
            removeEntities()
        }
        resetObjectives()
    }

    @Synchronized
    fun removePlayer(player: Player, team: WarTeam) {
        team.removePlayer(player)
        removePlayer(player)
    }

    private fun removePlayer(player: Player) {
        objectives.values.forEach {
            it.handleLeave(player)
        }

        // Remove player before restoring their state so the teleport doesn't get canceled
        val state = plugin.playerManager.getPlayerInfo(player)
        plugin.playerManager.removePlayer(player)
        state?.state?.restore(player)
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
    private fun addPlayer(player: Player, team: WarTeam): Boolean {
        assert(!team.isFull())
        team.addPlayer(player)
        plugin.playerManager.savePlayerState(player, team)
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = warzoneSettings.get(WarzoneConfigType.MAX_HEALTH)
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
            warzoneSettings.get(WarzoneConfigType.MAX_HEALTH)
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
        Bukkit.getScheduler().runTaskLater(plugin, { -> player.fireTicks = 0 }, 1)
        Bukkit.getScheduler().runTaskLater(plugin, { -> player.fireTicks = 0 }, 2)
        Bukkit.getScheduler().runTaskLater(plugin, { -> player.fireTicks = 0 }, 3)

        // Equip class
        playerInfo.warClass?.giveToPlayer(player)

        // Pick a random spawn
        val spawn = playerInfo.team.spawns.random()
        val spawnLocation = spawn.origin.clone()

        // Offset because the origin is in the ground
        spawnLocation.add(0.0, 1.0, 0.0)
        // We want players to be looking straight ahead
        spawnLocation.pitch = 0F

        player.velocity = Vector()
        player.teleport(spawnLocation)
        playerInfo.inSpawn = true
    }

    fun saveConfig() {
        val file = File("${plugin.dataFolder}/warzone-$name.yml")
        val config = YamlConfiguration()

        config.createSection("info")
        config.set("info.world", region.world.name)
        config.set("info.p1", region.p1.format(false))
        config.set("info.p2", region.p2.format(false))

        config.set("settings", warzoneSettings.config)
        config.set("team-settings", teamSettings.config)
        val teamsSection = config.createSection("teams")
        teams.toSortedMap().values.forEach {
            val teamSection = teamsSection.createSection(it.kind.name.toLowerCase())
            it.saveConfig(teamSection)
        }

        val objectivesSection = config.createSection("objectives")
        objectives.toSortedMap().values.forEach {
            val objectiveSection = objectivesSection.createSection(it.name)
            it.saveConfig(objectiveSection)
        }
        config.save(file)
    }

    fun unload() {
        restoreVolume()
        for ((_, team) in teams) {
            for (player in ImmutableList.copyOf(team.players)) {
                removePlayer(player, team)
            }
            team.reset()
        }
        resetObjectives()
    }

    fun saveVolume(): Result<Unit, WarError> {
        if (!plugin.hasPlugin("WorldEdit")) {
            return Err(WorldEditError("WorldEdit is not loaded"))
        }

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
        if (!plugin.hasPlugin("WorldEdit")) {
            return Err(WorldEditError("WorldEdit is not loaded"))
        }

        val clipboard = loadSchematic(volumePath)
        if (clipboard is Err) {
            return clipboard
        }
        val (x, y, z) = region.getMinimumPoint()
        val to = Location(region.world, x.toDouble(), y.toDouble(), z.toDouble())
        pasteSchematic(clipboard.unwrap(), to, false)
        return Ok(Unit)
    }

    @Synchronized
    private fun handleDeath(player: Player) {
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
        objectives.values.forEach {
            it.handleDeath(player)
        }
        val lives = playerInfo.team.lives
        if (lives == 0) {
            handleTeamLoss(playerInfo.team, player)
        } else {
            if (lives == 1) {
                broadcast("Team ${playerInfo.team}'s life pool is empty. One more death and they lose the battle!")
            }
            playerInfo.team.lives -= 1
            respawnPlayer(player)
        }
    }

    fun broadcast(message: String) {
        teams.values.forEach {
            it.broadcast(message)
        }
    }

    fun handleTeamLoss(team: WarTeam, player: Player) {
        val winningTeams = mutableListOf<WarTeam>()
        teams.values.filter {
            it != team
        }.forEach {
            it.addPoint()
            if (it.score >= it.settings.get(TeamConfigType.MAX_SCORE)) {
                winningTeams.add(it)
            }
        }
        broadcast("The battle is over. Team $team lost: ${player.name} died and there were no lives left in their life pool.")
        if (winningTeams.isEmpty()) {
            restoreVolume()
            reinitialize()
        } else {
            handleWin(winningTeams)
        }
    }

    fun handleWin(winningTeams: List<WarTeam>) {
        broadcast("Score cap reached. Game is over! Winning teams: ${winningTeams.joinToString()}")
        for ((_, team) in teams) {
            val won = team in winningTeams
            val econReward = getEconReward(team.settings.get(TeamConfigType.ECON_REWARD))
            for (player in team.players.toList()) {
                removePlayer(player, team)
            }
            if (won) {
                plugin.economy?.apply {
                    team.players.toList().forEach {
                        val response = depositPlayer(it, econReward)
                        if (response.transactionSuccess()) {
                            plugin.logger.warning("Failed to reward ${it.name}: ${response.errorMessage}")
                        }
                    }
                }
            }
        }
        restoreVolume()
        initialize()
    }

    fun handleSuicide(player: Player) {
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
        if (warzoneSettings.get(WarzoneConfigType.DEATH_MESSAGES)) {
            broadcast("${playerInfo.team.kind.chatColor}${player.name}${ChatColor.RESET} committed suicide")
        }
        handleDeath(player)
    }

    fun handleKill(attacker: Player, defender: Player, damager: Entity, direct: Boolean) {
        val attackerInfo = plugin.playerManager.getPlayerInfo(attacker) ?: return
        val defenderInfo = plugin.playerManager.getPlayerInfo(defender) ?: return

        if (warzoneSettings.get(WarzoneConfigType.DEATH_MESSAGES)) {
            val weapon = attacker.inventory.itemInMainHand
            val weaponName = if (weapon.hasItemMeta() && weapon.itemMeta!!.hasDisplayName()) {
                weapon.itemMeta!!.displayName
            } else if (weapon.type == Material.AIR) {
                "hand"
            } else {
                weapon.type.toString()
            }.toLowerCase().replace('_', ' ')
            val attackerColor = attackerInfo.team.kind.chatColor
            val defenderColor = defenderInfo.team.kind.chatColor
            val message =
                "${attackerColor}${attacker.name}${ChatColor.RESET}'s $weaponName killed ${defenderColor}${defender.name}${ChatColor.RESET}"
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

    fun onBlockBreak(player: Player?, block: Block): Boolean {
        objectives.values.forEach {
            if (it.handleBlockBreak(player, block)) {
                return true
            }
        }
        return false
    }

    fun onPlayerPickupItem(player: Player, item: Item): Boolean {
        objectives.values.forEach {
            if (it.handleItemPickup(player, item)) {
                return true
            }
        }
        return false
    }

    fun onPlayerMove(player: Player, from: Location, to: Location) {
        objectives.values.forEach {
            it.handlePlayerMove(player, from, to)
        }
    }

    fun onInventoryClick(player: Player, action: InventoryAction): Boolean {
        objectives.values.forEach {
            if (it.handleInventoryClick(player, action)) {
                return true
            }
        }
        return false
    }

    fun onPlayerDropItem(player: Player, item: Item): Boolean {
        objectives.values.forEach {
            if (it.handlePlayerDropItem(player, item)) {
                return true
            }
        }
        return false
    }

    fun onBlockPlace(player: Player, block: Block): Boolean {
        objectives.values.forEach {
            if (it.handleBlockPlace(player, block)) {
                return true
            }
        }
        return false
    }

    fun isSpawnBlock(block: Block): Boolean {
        for ((_, team) in teams) {
            for (spawn in team.spawns) {
                if (spawn.contains(block.location)) {
                    return true
                }
            }
        }
        return false
    }

    fun getFlagAtLocation(location: Location): FlagStructure? {
        val objective = objectives["flags"] as? FlagObjective ?: return null
        return objective.getFlagAtLocation(location)
    }

    fun addFlagObjective(location: Location, kind: TeamKind): Boolean {
        val flagStructure = FlagStructure(plugin, location, kind)
        teams.values.forEach { team ->
            team.spawns.forEach { spawn ->
                if (flagStructure.region.intersects(spawn.region)) {
                    return false
                }
            }
        }
        (objectives["flags"] as? FlagObjective)?.flags?.forEach { flag ->
            if (flagStructure.region.intersects(flag.region)) {
                return false
            }
        }
        (objectives["monuments"] as? MonumentObjective)?.monuments?.forEach { monument ->
            if (flagStructure.region.intersects(monument.region)) {
                return false
            }
        }
        flagStructure.saveVolume()
        flagStructure.build()
        addFlag(flagStructure)
        saveConfig()
        return true
    }

    fun addFlag(flag: FlagStructure) {
        val objective = objectives["flags"] as? FlagObjective ?: run {
            val temp = FlagObjective(plugin, this, mutableListOf())
            objectives[temp.name] = temp
            temp
        }
        objective.addFlag(flag)
    }

    fun removeFlag(flag: FlagStructure): Boolean {
        val objective = objectives["flags"] as? FlagObjective ?: return false
        return objective.removeFlag(flag)
    }

    fun getMonumentByName(name: String): MonumentStructure? {
        val objective = objectives["monuments"] as? MonumentObjective ?: return null
        return objective.monuments.firstOrNull { it.name.equals(name, true) }
    }

    fun getMonumentAtLocation(location: Location): MonumentStructure? {
        val objective = objectives["monuments"] as? MonumentObjective ?: return null
        return objective.getMonumentAtLocation(location)
    }

    fun addMonument(monument: MonumentStructure) {
        val objective = objectives["monuments"] as? MonumentObjective ?: run {
            val temp = MonumentObjective(plugin, this, mutableListOf())
            objectives[temp.name] = temp
            temp
        }
        objective.addMonument(monument)
    }

    fun removeMonument(monument: MonumentStructure): Boolean {
        val objective = objectives["monuments"] as? MonumentObjective ?: return false
        return objective.removeMonument(monument)
    }

    private fun resetObjectives() {
        objectives.values.forEach {
            it.reset()
        }
    }

    private fun getEconReward(base: Double): Double {
        val totalPlayers = numPlayers()
        val maxCapacity = maxPlayers()
        if (totalPlayers < 2) {
            return 0.0
        }
        return max(0.0, base + (base * (totalPlayers - 2) / (sqrt(maxCapacity.toDouble()) * 2)))
    }

    private fun removeEntities() {
        val minPoint = region.getMinimumPoint()
        val maxPoint = region.getMaximumPoint()
        val midPoint = Triple(
            ceil((maxPoint.first + minPoint.first) / 2.0),
            ceil((maxPoint.second + minPoint.second) / 2.0),
            ceil((maxPoint.third + minPoint.third) / 2.0)
        )
        val minLoc = Location(region.world, midPoint.first, midPoint.second, midPoint.third)
        minLoc.world?.getNearbyEntities(
            minLoc,
            maxPoint.first - midPoint.first,
            maxPoint.second - midPoint.second,
            maxPoint.third - midPoint.third
        )?.apply {
            forEach {
                if (it !is Player && region.contains(it.location)) {
                    it.remove()
                }
            }
        }
    }
}
