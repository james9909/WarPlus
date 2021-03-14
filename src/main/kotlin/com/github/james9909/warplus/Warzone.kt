package com.github.james9909.warplus

import com.github.james9909.warplus.config.CascadingConfig
import com.github.james9909.warplus.config.TeamConfigType
import com.github.james9909.warplus.config.WarConfigType
import com.github.james9909.warplus.config.WarzoneConfigType
import com.github.james9909.warplus.event.WarzoneEndEvent
import com.github.james9909.warplus.event.WarzoneJoinEvent
import com.github.james9909.warplus.event.WarzoneLeaveEvent
import com.github.james9909.warplus.event.WarzonePlayerDeathEvent
import com.github.james9909.warplus.event.WarzoneStartEvent
import com.github.james9909.warplus.extensions.blockLocation
import com.github.james9909.warplus.extensions.clearPotionEffects
import com.github.james9909.warplus.extensions.color
import com.github.james9909.warplus.extensions.format
import com.github.james9909.warplus.extensions.get
import com.github.james9909.warplus.extensions.pairs
import com.github.james9909.warplus.managers.WarParticipant
import com.github.james9909.warplus.objectives.BombObjective
import com.github.james9909.warplus.objectives.CapturePointObjective
import com.github.james9909.warplus.objectives.FlagObjective
import com.github.james9909.warplus.objectives.MonumentObjective
import com.github.james9909.warplus.objectives.Objective
import com.github.james9909.warplus.region.Region
import com.github.james9909.warplus.stat.StatTracker
import com.github.james9909.warplus.structures.BombStructure
import com.github.james9909.warplus.structures.CapturePointStructure
import com.github.james9909.warplus.structures.FlagStructure
import com.github.james9909.warplus.structures.MonumentStructure
import com.github.james9909.warplus.structures.TeamSpawnStructure
import com.github.james9909.warplus.structures.WarzonePortalStructure
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
import com.nisovin.magicspells.MagicSpells
import com.nisovin.magicspells.mana.ManaChangeReason
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.ItemStack

enum class WarzoneState {
    IDLING,
    RUNNING,
    EDITING,
    RESETTING
}

class Warzone(
    val plugin: WarPlus,
    val name: String,
    val region: Region,
    val teamSettings: CascadingConfig = CascadingConfig(),
    val warzoneSettings: CascadingConfig = CascadingConfig(),
    val objectives: HashMap<String, Objective> = hashMapOf(),
    val classes: List<String> = emptyList(),
    val reward: WarReward = WarReward.default(),
    private val portals: HashMap<String, WarzonePortalStructure> = hashMapOf()
) {
    var state = WarzoneState.IDLING
    val teams = HashMap<TeamKind, WarTeam>()
    val statTracker: StatTracker? = run {
        val dbm = plugin.databaseManager
        if (dbm != null && warzoneSettings.get(WarzoneConfigType.RECORD_STATS)) {
            StatTracker(plugin.playerManager, dbm)
        } else {
            null
        }
    }
    private val spectators = HashSet<Player>()
    private val configPath = "${plugin.dataFolder.absolutePath}/warzone-$name.yml"
    private val volumeFolder = "${plugin.dataFolder.absolutePath}/volumes/warzones"
    private val volumePath = "$volumeFolder/$name.schem"
    private val portalsByLocation: HashMap<String, WarzonePortalStructure> = hashMapOf()
    private var id: Int = -1

    init {
        portals.values.forEach {
            portalsByLocation[it.origin.blockLocation().format(direction = false)] = it
        }
    }

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
        val startEvent = WarzoneStartEvent(this)
        plugin.server.pluginManager.callEvent(startEvent)
        plugin.logger.info("Starting warzone $name")

        state = WarzoneState.RUNNING

        id = plugin.databaseManager?.addWarzone(name) ?: -1
        statTracker?.warzoneId = id

        // Add all joins retroactively upon warzone start since we now have a warzone ID.
        teams.forEach { (kind, team) ->
            team.players.forEach { player ->
                statTracker?.addJoin(player.uniqueId, kind)
            }
        }

        initialize(resetTeamScores = false)
        startObjectives()
    }

    private fun initialize(resetTeamScores: Boolean) {
        state = WarzoneState.RESETTING
        if (warzoneSettings.get(WarzoneConfigType.REMOVE_ENTITIES_ON_RESET)) {
            removeEntities()
        }
        teams.values.forEach { team ->
            team.resetAttributes(resetTeamScores)
            team.resetSpawns()
            team.players.forEach { player ->
                respawnPlayer(player)
            }
        }
        if (plugin.hasPlugin("FastAsyncWorldEdit")) {
            if (plugin.isEnabled) {
                plugin.server.scheduler.runTaskAsynchronously(plugin) { _ ->
                    val start = System.currentTimeMillis()
                    restoreVolume()
                    plugin.logger.info("Warzone volume reset took ${System.currentTimeMillis() - start} ms")
                    plugin.server.scheduler.runTask(plugin) { _ ->
                        resetObjectives()
                        state = if (resetTeamScores) {
                            WarzoneState.IDLING
                        } else {
                            WarzoneState.RUNNING
                        }
                        broadcast("Let the battle begin!")
                    }
                }
            } else {
                val start = System.currentTimeMillis()
                restoreVolume()
                plugin.logger.info("Warzone volume reset took ${System.currentTimeMillis() - start} ms")
                resetObjectives()
                state = if (resetTeamScores) {
                    WarzoneState.IDLING
                } else {
                    WarzoneState.RUNNING
                }
                broadcast("Let the battle begin!")
            }
        }
    }

    fun removePlayer(player: Player, team: WarTeam, showLeaveMessage: Boolean = true, finished: Boolean = false) {
        statTracker?.addLeave(player.uniqueId)
        team.removePlayer(player)
        removePlayer(player, finished)
        if (showLeaveMessage) {
            broadcast("${player.name} left the zone")
        }
        if (!finished) {
            // Maintain a maximum team size delta of 1
            balanceTeams()
        }
    }

    private fun removePlayer(player: Player, finished: Boolean = false) {
        val leaveEvent = WarzoneLeaveEvent(player, this)
        plugin.server.pluginManager.callEvent(leaveEvent)

        objectives.values.forEach { it.handleLeave(player) }

        // Remove player before restoring their state so the teleport doesn't get canceled
        val playerState = plugin.playerManager.getPlayerInfo(player.uniqueId)
        plugin.playerManager.removePlayer(player.uniqueId)
        playerState?.state?.restore(player)
        plugin.inventoryManager.restoreInventory(player)

        portals.forEach { it.value.updateBlocks() }
        if (
            numPlayers() == 0 &&
            state == WarzoneState.RUNNING &&
            warzoneSettings.get(WarzoneConfigType.RESET_ON_EMPTY) &&
            !finished
        ) {
            // Only reinitialize the zone if everyone left in the middle of the game
            plugin.logger.info("Last player left warzone $name. Reinitializing the warzone...")
            endZone(emptyList())
        }
    }

    fun addPlayer(player: Player): Boolean {
        if (numPlayers() >= maxPlayers()) {
            plugin.playerManager.sendMessage(player, Message.TOO_MANY_PLAYERS)
            return false
        }

        // Find candidate team to join
        val candidates = teams.values.sortedBy { it.size() }
        candidates.forEach { team ->
            if (!team.isFull()) {
                return addPlayer(player, team)
            }
        }
        plugin.playerManager.sendMessage(player, Message.TOO_MANY_PLAYERS)
        return false
    }

    private fun addPlayer(player: Player, team: WarTeam): Boolean {
        val joinEvent = WarzoneJoinEvent(player, this)
        plugin.server.pluginManager.callEvent(joinEvent)
        plugin.server.scheduler.runTaskAsynchronously(plugin) { _ ->
            plugin.databaseManager?.addPlayer(player.uniqueId)
        }

        assert(!team.isFull())
        team.addPlayer(player)
        plugin.playerManager.savePlayerState(
            player,
            team,
            plugin.config.get(WarConfigType.RESTORE_PLAYER_LOCATION)
        )
        portals.forEach { it.value.updateBlocks() }

        // Equip default class
        val defaultClass = team.settings.get(TeamConfigType.DEFAULT_CLASS).toLowerCase()
        val possibleClasses = team.resolveClasses().map { it.toLowerCase() }
        val className = if (defaultClass in possibleClasses) {
            // Equip specified with the default-class setting
            defaultClass
        } else {
            // Select the first class
            possibleClasses[0]
        }
        val warClass = plugin.classManager.getClass(className)
        if (warClass == null) {
            plugin.playerManager.sendMessage(player, "Failed to equip class $className")
            return false
        }
        equipClass(player, warClass, true)

        player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = warzoneSettings.get(WarzoneConfigType.MAX_HEALTH)
        respawnPlayer(player)
        broadcast("${player.name} joined team $team")

        if (state == WarzoneState.RUNNING) {
            // Only immediately add the join if the warzone is currently running.
            statTracker?.addJoin(player.uniqueId, team.kind)
        } else if (canStart()) {
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
            healthAttr.modifiers.forEach { modifier ->
                healthAttr.removeModifier(modifier)
            }
            healthAttr.baseValue
        }
        if (plugin.hasPlugin("MagicSpells")) {
            val maxMana = MagicSpells.getManaHandler().getMaxMana(player)
            MagicSpells.getManaHandler().setMana(player, maxMana, ManaChangeReason.POTION)
        }
        player.inventory.clear()
        PlayerState(
            health = maxHealth,
            maxHealth = maxHealth
        ).restore(player)
    }

    fun respawnPlayer(player: Player) {
        val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId) ?: return
        resetPlayer(player)
        Bukkit.getScheduler().runTaskLater(plugin, { -> player.fireTicks = 0 }, 1)
        Bukkit.getScheduler().runTaskLater(plugin, { -> player.fireTicks = 0 }, 2)
        Bukkit.getScheduler().runTaskLater(plugin, { -> player.fireTicks = 0 }, 3)

        // Equip class
        val warClass = playerInfo.warClass
        if (warClass != null) {
            equipClass(player, warClass, false)
        }

        // Pick a random spawn
        val spawn = playerInfo.team.spawns.random()
        spawn.teleport(player)

        playerInfo.inSpawn = true
    }

    fun saveConfig() {
        val file = File("${plugin.dataFolder}/warzone-$name.yml")
        val config = YamlConfiguration()

        config.createSection("info")
        config.set("info.world", region.world.name)
        config.set("info.p1", region.p1.format(false))
        config.set("info.p2", region.p2.format(false))

        if (classes.isNotEmpty()) {
            config.set("classes", classes)
        }
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

        val portalsSection = config.createSection("portals")
        portals.toSortedMap().values.forEach {
            val portalSection = portalsSection.createSection(it.name)
            portalSection.set("origin", it.origin.format())
        }

        val rewardSection = config.createSection("rewards")
        reward.saveConfig(rewardSection)

        config.save(file)
    }

    fun unload() {
        teams.values.forEach { team ->
            ImmutableList.copyOf(team.players).forEach { player ->
                removePlayer(player, team, finished = true)
            }
            team.reset()
        }
        resetObjectives()
    }

    fun delete(): Boolean {
        var success = true
        objectives.values.forEach { it.delete() }
        success = success && File(configPath).delete()
        success = success && File(volumePath).delete()
        return success
    }

    fun saveVolume(): Result<Unit, WarError> {
        if (!plugin.hasPlugin("FastAsyncWorldEdit")) {
            return Err(WorldEditError("FastAsyncWorldEdit is not loaded"))
        }

        val (minX, minY, minZ) = region.getMinimumPoint()
        val (maxX, maxY, maxZ) = region.getMaximumPoint()
        val region = CuboidRegion(
            BukkitAdapter.adapt(region.world),
            BlockVector3.at(minX, minY, minZ),
            BlockVector3.at(maxX, maxY, maxZ)
        )
        val clipboard = copyRegion(region)
        if (clipboard is Err) return clipboard
        saveClipboard(clipboard.unwrap(), volumePath)
        return Ok(Unit)
    }

    fun restoreVolume(): Result<Unit, WarError> {
        if (!plugin.hasPlugin("FastAsyncWorldEdit")) {
            return Err(WorldEditError("FastAsyncWorldEdit is not loaded"))
        }

        val clipboard = loadSchematic(volumePath)
        if (clipboard is Err) return clipboard
        val (x, y, z) = region.getMinimumPoint()
        val to = Location(region.world, x.toDouble(), y.toDouble(), z.toDouble())
        pasteSchematic(clipboard.unwrap(), to, false)
        return Ok(Unit)
    }

    private fun handleDeath(player: Player, entity: Entity?, cause: DamageCause) {
        val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId) ?: return
        val deathEvent = WarzonePlayerDeathEvent(player, this, entity, cause)
        plugin.server.pluginManager.callEvent(deathEvent)

        statTracker?.addDeath(player.uniqueId)

        objectives.values.forEach { it.handleDeath(player) }
        val team = playerInfo.team
        val lives = team.lives
        if (lives == 0) {
            handleTeamLoss(team, player)
        } else {
            if (lives == 1) {
                broadcast("Team $team's life pool is empty. One more death and they lose the battle!")
            }
            team.lives -= 1
            team.spawns.forEach { it.updateSign(team) }
            respawnPlayer(player)
        }
    }

    fun broadcast(message: String) {
        teams.values.forEach {
            it.broadcast(message)
        }
    }

    private fun teamLossMessage(team: WarTeam, player: Player): String {
        return "The battle is over. " +
            "Team $team lost: ${player.name} died and there were no lives left in their life pool."
    }

    private fun handleTeamLoss(team: WarTeam, player: Player) {
        val winningTeams = mutableListOf<TeamKind>()
        teams.values.filter {
            it != team
        }.forEach {
            it.addPoint()
            if (it.score >= it.settings.get(TeamConfigType.MAX_SCORE)) {
                winningTeams.add(it.kind)
            }
        }
        broadcast(teamLossMessage(team, player))
        if (winningTeams.isEmpty()) {
            initialize(resetTeamScores = false)
        } else {
            handleWin(winningTeams)
        }
    }

    fun handleWin(winningTeams: List<TeamKind>) {
        val numPlayers = numPlayers()
        val maxPlayers = maxPlayers()
        teams.values.forEach { team ->
            val won = team.kind in winningTeams
            val (econWinReward, econLossReward) = getEconRewards(team.settings.get(TeamConfigType.ECON_REWARD), numPlayers, maxPlayers)
            val teamPlayers = team.players.toList()
            val (mostKills, mostHeals, mostPoints) = if (teamPlayers.size >= team.settings.get(TeamConfigType.MIN_PLAYERS_FOR_MVP)) {
                Triple(
                    statTracker?.maxStatsBy(team.kind) { it.kills }?.run {
                        statTracker.addMvp(first)
                        Pair(plugin.server.getOfflinePlayer(first), second)
                    },
                    statTracker?.maxStatsBy(team.kind) { it.heals }?.run {
                        statTracker.addMvp(first)
                        Pair(plugin.server.getOfflinePlayer(first), second)
                    },
                    statTracker?.maxStatsBy(team.kind) { it.flagCaptures + it.bombs }?.run {
                        statTracker.addMvp(first)
                        Pair(plugin.server.getOfflinePlayer(first), second)
                    })
            } else {
                Triple(null, null, null)
            }
            teamPlayers.forEach { player ->
                removePlayer(player, team, showLeaveMessage = false, finished = true)

                val econReward = if (won) {
                    reward.giveWinReward(player)
                    statTracker?.addWin(player.uniqueId)
                    econWinReward
                } else {
                    reward.giveLossReward(player)
                    statTracker?.addLoss(player.uniqueId)
                    econLossReward
                }

                // Handle mvp rewards separately
                if (player.uniqueId == mostKills?.first?.uniqueId) {
                    reward.giveMvpReward(player)
                }
                if (player.uniqueId == mostHeals?.first?.uniqueId) {
                    reward.giveMvpReward(player)
                }
                if (player.uniqueId == mostPoints?.first?.uniqueId) {
                    reward.giveMvpReward(player)
                }

                plugin.economy?.apply {
                    val response = depositPlayer(player, econReward)
                    if (!response.transactionSuccess()) {
                        plugin.logger.warning("Failed to reward ${player.name}: ${response.errorMessage}")
                    }
                }
                sendFinalResults(player, winningTeams, econReward, mostKills, mostHeals, mostPoints)
            }
        }
        spectators.forEach { removeSpectator(it) }
        endZone(winningTeams)
    }

    fun handleSuicide(player: Player, cause: DamageCause) {
        val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId) ?: return
        if (warzoneSettings.get(WarzoneConfigType.DEATH_MESSAGES)) {
            broadcast("${playerInfo.team.kind.chatColor}${player.name}${ChatColor.RESET} committed suicide")
        }
        handleDeath(player, player, cause)
    }

    fun handleKill(attacker: Player, defender: Player, damager: Entity, cause: DamageCause, direct: Boolean) {
        val attackerInfo = plugin.playerManager.getPlayerInfo(attacker.uniqueId) ?: return
        val defenderInfo = plugin.playerManager.getPlayerInfo(defender.uniqueId) ?: return

        if (warzoneSettings.get(WarzoneConfigType.DEATH_MESSAGES)) {
            val attackerColor = attackerInfo.team.kind.chatColor
            val defenderColor = defenderInfo.team.kind.chatColor
            val formattedAttacker = "${attackerColor}${attacker.name}${ChatColor.RESET}"
            val formattedDefender = "${defenderColor}${defender.name}${ChatColor.RESET}"
            val message = if (!direct) {
                "$formattedAttacker killed $formattedDefender"
            } else {
                val weapon = attacker.inventory.itemInMainHand
                val weaponName = if (weapon.hasItemMeta() && weapon.itemMeta!!.hasDisplayName()) {
                    "${weapon.itemMeta!!.displayName}${ChatColor.RESET}"
                } else if (weapon.type == Material.AIR) {
                    "hand"
                } else {
                    weapon.type.toString()
                }.toLowerCase().replace('_', ' ')
                "$formattedAttacker's $weaponName killed $formattedDefender"
            }
            broadcast(message)
        }

        statTracker?.apply {
            val attackerClass = attackerInfo.warClass
            val defenderClass = defenderInfo.warClass
            // This check should not be necessary, but let's be safe
            if (attackerClass != null && defenderClass != null) {
                addKill(attacker.uniqueId, defender.uniqueId, attackerClass, defenderClass)
            }
        }
        handleDeath(defender, attacker, cause)
    }

    fun handleNaturalDeath(player: Player, cause: DamageCause) {
        val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId) ?: return
        when (val damager = playerInfo.lastDamager.damager) {
            is Player -> {
                handleKill(damager, player, damager, cause, false)
                return
            }
            null -> {
            }
        }
        val playerString = "${playerInfo.team.kind.chatColor}${player.name}${ChatColor.RESET}"
        val message = when (cause) {
            DamageCause.BLOCK_EXPLOSION -> "$playerString exploded"
            DamageCause.FIRE,
            DamageCause.FIRE_TICK,
            DamageCause.LAVA,
            DamageCause.LIGHTNING -> "$playerString burned to a crisp"
            DamageCause.DROWNING -> "$playerString drowned"
            DamageCause.FALL -> "$playerString fell to an untimely death"
            DamageCause.SUFFOCATION -> "$playerString suffocated"
            DamageCause.PROJECTILE -> "$playerString was shot to death"
            DamageCause.FALLING_BLOCK -> "$playerString was crushed"
            DamageCause.CONTACT -> "$playerString got too close to a cactus"
            DamageCause.CUSTOM -> "$playerString was killed by the void"
            else -> {
                plugin.logger.info("Unhandled cause of death: $cause")
                "$playerString died"
            }
        }
        broadcast(message)
        handleDeath(player, null, cause)
    }

    fun handleMobDeath(player: Player, entity: LivingEntity, cause: DamageCause) {
        val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId) ?: return
        val playerString = "${playerInfo.team.kind.chatColor}${player.name}${ChatColor.RESET}"
        broadcast("$playerString was slain by ${entity.name}")
        handleDeath(player, entity, cause)
    }

    // Whether or not the location is spawn protected from the point of view of `entity`
    private fun isSpawnProtectedAsEntity(entity: Entity?, location: Location): Boolean {
        val spawnRadius = warzoneSettings.get(WarzoneConfigType.SPAWN_PROTECTION_RADIUS).toDouble().pow(2)
        if (spawnRadius <= 1) return false
        teams.values.forEach { team ->
            team.spawns.forEach { spawn ->
                if (spawn.origin.distanceSquared(location) <= spawnRadius) {
                    if (entity is Player) {
                        val playerInfo = plugin.playerManager.getPlayerInfo(entity.uniqueId) ?: return true
                        // Players can modify the blocks within the radius of their own spawns
                        return playerInfo.team != team
                    }
                    return true
                }
            }
        }
        return false
    }

    fun onBlockBreak(player: Player?, block: Block): Boolean {
        objectives.values.forEach {
            if (it.handleBlockBreak(player, block)) {
                return true
            }
        }
        // Allow objectives to handle block breaks otherwise
        // objectives like flags and monuments won't work
        return isSpawnProtectedAsEntity(player, block.location)
    }

    fun onPlayerPickupItem(player: Player, item: Item): Boolean = objectives.values.any {
        it.handleItemPickup(player, item)
    }

    fun onPlayerMove(player: Player, from: Location, to: Location) = objectives.values.forEach {
        it.handlePlayerMove(player, from, to)
    }

    fun onInventoryClick(player: Player, action: InventoryAction): Boolean = objectives.values.any {
        it.handleInventoryClick(player, action)
    }

    fun onPlayerDropItem(player: Player, item: Item): Boolean = objectives.values.any {
        it.handlePlayerDropItem(player, item)
    }

    fun onBlockPlace(entity: Entity?, block: Block): Boolean {
        objectives.values.forEach {
            if (it.handleBlockPlace(entity, block)) {
                return true
            }
        }
        // Allow objectives to handle block breaks otherwise
        // objectives like flags and monuments won't work
        return isSpawnProtectedAsEntity(entity, block.location)
    }

    fun onSpellCast(player: Player): Boolean = objectives.values.any {
        it.handleSpellCast(player)
    }

    fun onEntityBlockChange(entity: Entity, block: Block): Boolean = objectives.values.any {
        it.handleBlockChange(entity, block)
    }

    fun isSpawnBlock(block: Block): Boolean = teams.values.any { team ->
        team.spawns.any { spawn ->
            spawn.contains(block.location)
        }
    }

    fun getFlagAtLocation(location: Location): FlagStructure? {
        val objective = objectives["flags"] as? FlagObjective ?: return null
        return objective.getFlagAtLocation(location)
    }

    private fun validateStructureRegion(objectiveRegion: Region): Result<Unit, WarError> {
        if (!region.contains(objectiveRegion)) {
            return Err(WarStructureError("Structures must be fully contained within the warzone"))
        }
        teams.values.forEach { team ->
            team.spawns.forEach { spawn ->
                if (objectiveRegion.intersects(spawn.region)) {
                    return Err(WarStructureError("A structure cannot overlap with a spawn"))
                }
            }
        }
        (objectives["flags"] as? FlagObjective)?.flags?.forEach { flag ->
            if (objectiveRegion.intersects(flag.region)) {
                return Err(WarStructureError("A structure cannot overlap with a flag"))
            }
        }
        (objectives["monuments"] as? MonumentObjective)?.monuments?.forEach { monument ->
            if (objectiveRegion.intersects(monument.region)) {
                return Err(WarStructureError("A structure cannot overlap with a monument"))
            }
        }
        (objectives["capture_points"] as? CapturePointObjective)?.capturePoints?.forEach { cp ->
            if (objectiveRegion.intersects(cp.region)) {
                return Err(WarStructureError("A structure cannot overlap with a capture point"))
            }
        }
        return Ok(Unit)
    }

    fun addFlagObjective(location: Location, kind: TeamKind): Result<Unit, WarError> {
        val flagStructure = FlagStructure(plugin, location, kind)
        val result = validateStructureRegion(flagStructure.region)
        when (result) {
            is Ok -> {
                flagStructure.saveVolume()
                flagStructure.build()
                addFlag(flagStructure)
                saveConfig()
            }
            is Err -> {
            } // Do nothing, just return
        }
        return result
    }

    private fun addFlag(flag: FlagStructure) {
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

    fun addMonumentObjective(location: Location, name: String): Result<Unit, WarError> {
        val monumentStructure =
            MonumentStructure(plugin, location, name)
        val result = validateStructureRegion(monumentStructure.region)
        when (result) {
            is Ok -> {
                monumentStructure.saveVolume()
                monumentStructure.build()
                addMonument(monumentStructure)
                saveConfig()
            }
            is Err -> {
            } // Do nothing, just return
        }
        return result
    }

    private fun addMonument(monument: MonumentStructure) {
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

    fun addTeamSpawn(origin: Location, kind: TeamKind): Result<Unit, WarError> {
        val team = teams[kind] ?: run {
            val newTeam = WarTeam(kind, mutableListOf(), this)
            addTeam(newTeam)
            newTeam
        }
        val spawnStyle = try {
            team.settings.get(TeamConfigType.SPAWN_STYLE)
        } catch (e: IllegalArgumentException) {
            return Err(WarStructureError("Invalid spawn style for $team"))
        }
        val teamSpawn = TeamSpawnStructure(plugin, origin, team.kind, spawnStyle)
        val result = validateStructureRegion(teamSpawn.region)
        when (result) {
            is Ok -> {
                teamSpawn.saveVolume()
                teamSpawn.build()
                teamSpawn.updateSign(team)
                team.addTeamSpawn(teamSpawn)
                saveConfig()
            }
            is Err -> { /*Do nothing, just return*/
            }
        }
        return result
    }

    fun removeTeamSpawn(spawn: TeamSpawnStructure) {
        val team = teams[spawn.kind] ?: return
        team.spawns.remove(spawn)
        if (team.spawns.isEmpty()) {
            teams.remove(team.kind)
        }
    }

    private fun resetObjectives() = objectives.values.forEach {
        it.reset()
    }

    private fun startObjectives() = objectives.values.forEach {
        it.start()
    }

    private fun stopObjectives() = objectives.values.forEach {
        it.stop()
    }

    private fun roundToDecimal(number: Double): Double {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(number).toDouble()
    }

    private fun getEconRewards(base: Double, numPlayers: Int, maxPlayers: Int): Pair<Double, Double> {
        if (numPlayers < 2) return Pair(0.0, 0.0)
        val result = base + (base * (numPlayers - 2) / (sqrt(maxPlayers.toDouble()) * 2))
        val winReward = max(0.0, roundToDecimal(result))
        return Pair(winReward, roundToDecimal(winReward / 4))
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
        )?.onEach {
            if (it !is Player && region.contains(it.location)) {
                it.remove()
            }
        }
    }

    fun pruneStructures(): Boolean {
        var pruned = false
        teams.entries.removeIf { (_, team) ->
            team.spawns.retainAll { spawn ->
                if (region.contains(spawn.region)) {
                    return@retainAll true
                }
                spawn.restoreVolume()
                pruned = true
                return@retainAll false
            }
            team.spawns.isEmpty()
        }
        (objectives["bombs"] as? BombObjective)?.bombs?.retainAll { bomb ->
            if (region.contains(bomb.region)) {
                return@retainAll true
            }
            bomb.restoreVolume()
            pruned = true
            return@retainAll false
        }
        (objectives["capture_points"] as? CapturePointObjective)?.capturePoints?.retainAll { cp ->
            if (region.contains(cp.region)) {
                return@retainAll true
            }
            cp.restoreVolume()
            pruned = true
            return@retainAll false
        }
        (objectives["flags"] as? FlagObjective)?.flags?.retainAll { flag ->
            if (teams[flag.kind] != null && region.contains(flag.region)) {
                return@retainAll true
            }
            flag.restoreVolume()
            pruned = true
            return@retainAll false
        }
        (objectives["monuments"] as? MonumentObjective)?.monuments?.retainAll { monument ->
            if (region.contains(monument.region)) {
                return@retainAll true
            }
            monument.restoreVolume()
            pruned = true
            return@retainAll false
        }
        return pruned
    }

    fun resolveClasses(): List<String> {
        if (classes.isNotEmpty()) {
            return classes
        }
        return plugin.classManager.resolveClasses()
    }

    fun setHelmet(player: Player, playerInfo: WarParticipant.Player) {
        if (warzoneSettings.get(WarzoneConfigType.BLOCK_HEADS)) {
            player.inventory.helmet = ItemStack(playerInfo.team.kind.material)
        }
    }

    fun equipClass(player: Player, warClass: WarClass, updatePlayerInfo: Boolean) {
        val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId) ?: return

        warClass.giveToPlayer(player)
        setHelmet(player, playerInfo)

        if (updatePlayerInfo) {
            playerInfo.warClass = warClass
        }
    }

    fun addPortal(portal: WarzonePortalStructure) {
        portals[portal.name.toLowerCase()] = portal
        portalsByLocation[portal.origin.blockLocation().format(direction = false)] = portal
    }

    fun removePortal(portal: WarzonePortalStructure) {
        portals.remove(portal.name.toLowerCase())
        portalsByLocation.remove(portal.origin.blockLocation().format(direction = false))
    }

    fun getPortalByLocation(location: Location): WarzonePortalStructure? {
        return portalsByLocation[location.blockLocation().format(direction = false)]
    }

    fun getPortalByName(name: String): WarzonePortalStructure? {
        return portals[name.toLowerCase()]
    }

    fun getPortals(): List<WarzonePortalStructure> {
        return portals.values.toList()
    }

    fun addSpectator(player: Player) {
        plugin.playerManager.saveSpectatorState(
            player,
            this,
            plugin.config.get(WarConfigType.RESTORE_PLAYER_LOCATION)
        )
        spectators.add(player)
        val permissions = plugin.playerManager.getPermissions(player)
        permissions.setPermission("magicspells.notarget", true)

        val team = teams.entries.elementAt(Random.nextInt(teams.size)).value

        // Pick a random spawn
        val spawn = team.spawns.random()
        spawn.teleport(player)

        player.gameMode = GameMode.SPECTATOR
    }

    fun removeSpectator(player: Player) {
        val spectator = plugin.playerManager.getSpectatorInfo(player.uniqueId) ?: return
        spectators.remove(player)
        plugin.playerManager.removePlayer(player.uniqueId)

        val permissions = plugin.playerManager.getPermissions(player)
        permissions.unsetPermission("magicspells.notarget")

        spectator.state.restore(player)
    }

    fun addCapturePointObjective(location: Location, name: String): Result<Unit, WarError> {
        val cp =
            CapturePointStructure(plugin, location, name)
        val result = validateStructureRegion(cp.region)
        when (result) {
            is Ok -> {
                cp.saveVolume()
                cp.build()
                addCapturePoint(cp)
                saveConfig()
            }
            is Err -> {
            } // Do nothing, just return
        }
        return result
    }

    private fun addCapturePoint(cp: CapturePointStructure) {
        val objective = objectives["capture_points"] as? CapturePointObjective ?: run {
            val temp = CapturePointObjective(plugin, this, mutableListOf())
            objectives[temp.name] = temp
            temp
        }
        objective.addCapturePoint(cp)
    }

    fun removeCapturePoint(cp: CapturePointStructure): Boolean {
        val objective = objectives["capture_points"] as? CapturePointObjective ?: return false
        return objective.removeCapturePoint(cp)
    }

    fun getCapturePointAtLocation(location: Location): CapturePointStructure? {
        val objective = objectives["capture_points"] as? CapturePointObjective ?: return null
        return objective.getCapturePointAtLocation(location)
    }

    fun getCapturePointByName(name: String): CapturePointStructure? {
        val objective = objectives["capture_points"] as? CapturePointObjective ?: return null
        return objective.capturePoints.firstOrNull { it.name.equals(name, true) }
    }

    fun addBombObjective(location: Location, name: String): Result<Unit, WarError> {
        val bomb =
            BombStructure(plugin, location, name)
        val result = validateStructureRegion(bomb.region)
        when (result) {
            is Ok -> {
                bomb.saveVolume()
                bomb.build()
                addBomb(bomb)
                saveConfig()
            }
            is Err -> {
            } // Do nothing, just return
        }
        return result
    }

    private fun addBomb(cp: BombStructure) {
        val objective = objectives["bombs"] as? BombObjective ?: run {
            val temp = BombObjective(plugin, this, mutableListOf())
            objectives[temp.name] = temp
            temp
        }
        objective.addBomb(cp)
    }

    fun removeBomb(cp: BombStructure): Boolean {
        val objective = objectives["bombs"] as? BombObjective ?: return false
        return objective.removeBomb(cp)
    }

    fun getBombAtLocation(location: Location): BombStructure? {
        val objective = objectives["bombs"] as? BombObjective ?: return null
        return objective.getBombAtLocation(location)
    }

    fun getBombByName(name: String): BombStructure? {
        val objective = objectives["bombs"] as? BombObjective ?: return null
        return objective.bombs.firstOrNull { it.name.equals(name, true) }
    }

    private fun balanceTeams() {
        val teams = teams.values.toList()
        for ((t1, t2) in teams.pairs()) {
            if (abs(t1.players.size - t2.players.size) >= 2) {
                val (oldTeam, newTeam) = if (t1.players.size > t2.players.size) {
                    Pair(t1, t2)
                } else {
                    Pair(t2, t1)
                }
                if (newTeam.isFull()) {
                    // Adhere to team size limits
                    continue
                }
                val eject = oldTeam.players.random()
                moveToTeam(eject, oldTeam, newTeam)
                return
            }
        }
    }

    // Move a player to another team without doing all the work of removing them from the warzone
    // and then adding them back
    private fun moveToTeam(player: Player, oldTeam: WarTeam, newTeam: WarTeam) {
        oldTeam.removePlayer(player)
        objectives.values.forEach {
            it.handleLeave(player)
        }
        statTracker?.apply {
            // Switching teams via autobalance is best represented as a leave-then-join
            addLeave(player.uniqueId)
            addJoin(player.uniqueId, newTeam.kind)
        }
        newTeam.addPlayer(player)
        val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId) ?: return
        plugin.playerManager.setPlayerInfo(player.uniqueId, playerInfo.copy(team = newTeam))
        respawnPlayer(player)
        broadcast("[Auto Balance] - ${player.name} was moved to $newTeam")
    }

    private fun endZone(winningTeams: List<TeamKind>) {
        val endEvent = WarzoneEndEvent(this)
        plugin.server.pluginManager.callEvent(endEvent)
        plugin.logger.info("Ending warzone $name, winners: ${winningTeams.joinToString(",")}")

        // Write all stats to the database
        statTracker?.apply {
            plugin.logger.info("Writing stats to the database...")
            if (plugin.isEnabled) {
                plugin.server.scheduler.runTaskAsynchronously(plugin) { _ ->
                    flush()
                    plugin.logger.info("Stats flushed.")
                }
            } else {
                flush()
                plugin.logger.info("Stats flushed.")
            }
        }

        initialize(resetTeamScores = true)

        plugin.databaseManager?.endWarzone(id, winningTeams)
        id = -1
        statTracker?.warzoneId = id

        stopObjectives()
    }

    private fun sendFinalResults(
        player: Player,
        winners: List<TeamKind>,
        econReward: Double,
        mostKills: Pair<OfflinePlayer, Int>?,
        mostHeals: Pair<OfflinePlayer, Int>?,
        mostPoints: Pair<OfflinePlayer, Int>?
    ) {
        val losers = teams.keys.filter { !winners.contains(it) }
        plugin.playerManager.sendMessage(player, "&8&m----------------------------------------".color(), withPrefix = false)
        plugin.playerManager.sendMessage(player, "               &d&lWarzone Over".color(), withPrefix = false)
        plugin.playerManager.sendMessage(player, "    &a&lWinner: ${winners.joinToString(", ") { it.format() }}        &c&lLoser: ${losers.joinToString(", ") { it.format() }}".color(), withPrefix = false)
        plugin.playerManager.sendMessage(player, "&7 ".color(), withPrefix = false)
        if (mostKills != null || mostHeals != null || mostPoints != null) {
            val killsDisplay = mostKills?.run {
                "&f${first.name} &7[&4$second&7]"
            } ?: "Nobody"
            val healsDisplay = mostHeals?.run {
                "&f${first.name} &7[&4$second&7]"
            } ?: "Nobody"
            val pointsDisplay = mostPoints?.run {
                "&f${first.name} &7[&4$second&7]"
            } ?: "Nobody"
            plugin.playerManager.sendMessage(player, "  &bMost Kills: $killsDisplay &d|| &bMost Heals: $healsDisplay".color(), withPrefix = false)
            plugin.playerManager.sendMessage(player, "            &bMost Points: &f$pointsDisplay".color(), withPrefix = false)
            plugin.playerManager.sendMessage(player, "&7 ".color(), withPrefix = false)
        }
        plugin.playerManager.sendMessage(player, "               &6You earned &a${'$'}$econReward".color(), withPrefix = false)
        plugin.playerManager.sendMessage(player, "&8&m----------------------------------------".color(), withPrefix = false)
    }

    fun isFlagThief(player: Player): Boolean = (objectives["flags"] as? FlagObjective)?.isFlagThief(player) ?: false

    fun isBombCarrier(player: Player): Boolean = (objectives["bombs"] as? BombObjective)?.isBombCarrier(player) ?: false
}
