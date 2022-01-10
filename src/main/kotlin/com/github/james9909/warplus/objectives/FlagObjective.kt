package com.github.james9909.warplus.objectives

import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.config.TeamConfigType
import com.github.james9909.warplus.config.WarzoneConfigType
import com.github.james9909.warplus.extensions.clearPotionEffects
import com.github.james9909.warplus.extensions.format
import com.github.james9909.warplus.extensions.toLocation
import com.github.james9909.warplus.structures.FlagStructure
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

fun createFlagObjective(plugin: WarPlus, warzone: Warzone, config: ConfigurationSection): FlagObjective {
    val flags = mutableListOf<FlagStructure>()
    config.getMapList("locations").forEach { flagMap ->
        val teamKind: TeamKind
        try {
            teamKind = TeamKind.valueOf((flagMap["team"] as String).uppercase())
        } catch (e: IllegalArgumentException) {
            return@forEach
        }

        val origin = (flagMap["origin"] as String).toLocation()
        flags.add(
            FlagStructure(
                plugin,
                origin,
                teamKind
            )
        )
    }
    return FlagObjective(
        plugin,
        warzone,
        flags
    )
}

class FlagObjective(
    private val plugin: WarPlus,
    private val warzone: Warzone,
    val flags: MutableList<FlagStructure>
) : Objective(plugin, warzone) {
    override val name: String = "flags"

    private val flagThieves = HashMap<Player, FlagStructure>()

    fun addFlag(flag: FlagStructure) = flags.add(flag)

    fun removeFlag(flag: FlagStructure) = flags.remove(flag)

    fun getFlagAtLocation(location: Location): FlagStructure? = flags.firstOrNull { it.contains(location) }

    override fun handleBlockBreak(player: Player?, block: Block): Boolean {
        val flagStructure = flags.find {
            it.contains(block.location)
        } ?: return false
        if (player == null) return true

        val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId) ?: return true
        if (playerInfo.team.warzone != warzone) return true
        if (playerInfo.team.kind == flagStructure.kind) return true
        if (block != flagStructure.flagBlock) return true
        if (flagThieves.containsKey(player)) return true
        if (warzone.isBombCarrier(player)) return true

        return stealFlag(player, flagStructure)
    }

    override fun handleBlockPlace(entity: Entity?, block: Block): Boolean {
        if (entity is Player && flagThieves.containsKey(entity)) {
            return true
        }
        return flags.any { it.contains(block.location) }
    }

    override fun handleItemPickup(player: Player, item: Item): Boolean = flagThieves.containsKey(player)

    override fun handlePlayerMove(player: Player, from: Location, to: Location) {
        if (!flagThieves.containsKey(player)) {
            return
        }
        val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId) ?: return
        val team = playerInfo.team
        val inSpawn = team.spawns.any {
            it.contains(to)
        }
        if (!inSpawn) {
            return
        }
        val flag = flagThieves[player] ?: return // Null case should never happen
        val otherTeam = warzone.teams[flag.kind]
        if (otherTeam != null && !otherTeam.hasEnoughPlayers()) {
            plugin.playerManager.sendMessage(
                player,
                "You can't capture $otherTeam's flag since there are not enough players on that team!"
            )
            return
        }

        val ourFlagStolen = flagThieves.values.any {
            it.kind == team.kind
        }
        if (ourFlagStolen) {
            plugin.playerManager.sendMessage(
                player,
                "You can't capture $otherTeam's flag until your team's flag is returned!"
            )
            return
        }

        flag.build()
        team.warzone.broadcast("${player.name} captured ${flag.kind.format()}'s flag. Team $team scores one point.")
        team.addPoint()
        team.warzone.statTracker?.apply {
            addFlagCapture(player.uniqueId)
        }

        // Detect win condition
        if (team.score >= team.settings.get(TeamConfigType.MAX_SCORE)) {
            team.warzone.handleWin(listOf(team.kind))
            return
        }
        flagThieves.remove(player)
        team.warzone.respawnPlayer(player)
    }

    override fun handleInventoryClick(player: Player, action: InventoryAction): Boolean =
        flagThieves.containsKey(player)

    override fun handlePlayerDropItem(player: Player, item: Item): Boolean = flagThieves.containsKey(player)

    override fun handleSpellCast(player: Player): Boolean = flagThieves.containsKey(player)

    override fun handleDeath(player: Player) = dropFlag(player)

    override fun handleLeave(player: Player) = dropFlag(player)

    private fun dropFlag(player: Player) {
        if (!flagThieves.containsKey(player)) {
            return
        }
        val flag = flagThieves.remove(player) ?: return
        flag.build()
        warzone.broadcast("${player.name} has dropped ${flag.kind.format()}'s flag!")
    }

    private fun stealFlag(player: Player, flag: FlagStructure): Boolean {
        val team = warzone.teams[flag.kind] ?: return false
        if (!team.hasEnoughPlayers()) {
            plugin.playerManager.sendMessage(
                player,
                "You can't steal $team's flag since there are not enough players on that team!"
            )
            return true
        }
        flag.flagBlock.type = Material.AIR
        flagThieves[player] = flag

        // Fill the player's inventory with wool
        val contents = player.inventory.storageContents
        contents.forEachIndexed { i, _ ->
            contents[i] = ItemStack(flag.kind.material, 64)
        }
        player.inventory.storageContents = contents
        player.inventory.setItemInOffHand(null)

        player.clearPotionEffects()
        if (warzone.warzoneSettings.get(WarzoneConfigType.GLOW_FLAG_THIEVES)) {
            player.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1))
        }
        warzone.broadcast("${player.name} stole team $team's flag")
        return false
    }

    override fun saveConfig(config: ConfigurationSection) {
        config.set(
            "locations",
            flags.map {
                mapOf(
                    "origin" to it.origin.format(),
                    "team" to it.kind.toString().lowercase()
                )
            }
        )
    }

    override fun reset() {
        flags.forEach {
            it.build()
        }
        flagThieves.clear()
    }

    override fun delete() {
        flags.forEach { it.deleteVolume() }
    }

    fun isFlagThief(player: Player): Boolean = flagThieves.containsKey(player)
}
