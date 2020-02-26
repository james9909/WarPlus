package com.github.james9909.warplus.objectives

import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.extensions.clearPotionEffects
import com.github.james9909.warplus.extensions.toLocation
import com.github.james9909.warplus.structure.FlagStructure
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.ItemStack

fun createFlagObjective(plugin: WarPlus, warzone: Warzone, config: ConfigurationSection): FlagObjective? {
    val flags = mutableListOf<FlagStructure>()
    config.getMapList("locations").forEach { flagMap ->
        val teamKind: TeamKind
        try {
            teamKind = TeamKind.valueOf((flagMap["team"] as String).toUpperCase())
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
    val flagStructures: MutableList<FlagStructure>
) : AbstractObjective(plugin, warzone) {
    override val name: String = "flags"

    private val flagThieves = HashMap<Player, FlagStructure>()

    fun addFlag(flag: FlagStructure) {
        flagStructures.add(flag)
    }

    fun removeFlag(flag: FlagStructure) = flagStructures.remove(flag)

    fun getFlagAtLocation(location: Location): FlagStructure? {
        return flagStructures.firstOrNull { it.contains(location) }
    }

    override fun handleBlockBreak(player: Player?, block: Block): Boolean {
        val flagStructure = flagStructures.find {
            it.contains(block.location)
        } ?: return false
        if (player == null) {
            return true
        }
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return true
        if (playerInfo.team.kind == flagStructure.kind ||
            block != flagStructure.flagBlock ||
            flagThieves.containsKey(player)
        ) {
            return true
        }
        stealFlag(player, flagStructure)
        return false
    }

    override fun handleBlockPlace(player: Player, block: Block): Boolean = flagThieves.containsKey(player)

    override fun handleItemPickup(player: Player, item: Item): Boolean = flagThieves.containsKey(player)

    override fun handlePlayerMove(player: Player, from: Location, to: Location) {
        if (!flagThieves.containsKey(player)) {
            return
        }
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
        val team = playerInfo.team
        val inSpawn = team.spawns.any {
            it.contains(to)
        }
        if (inSpawn) {
            val flag = flagThieves[player] ?: return // Null case should never happen
            flag.build()
            team.warzone.broadcast("${player.name} captured ${flag.kind.format()}'s flag. Team $team scores one point.")
            team.addPoint()

            // Detect win condition
            if (team.score >= team.settings.getInt("max-score", 2)) {
                team.warzone.handleWin(listOf(team))
                return
            }
            flagThieves.remove(player)
            team.warzone.respawnPlayer(player)
        }
    }

    override fun handleInventoryClick(player: Player, action: InventoryAction): Boolean =
        flagThieves.containsKey(player)

    override fun handlePlayerDropItem(player: Player, item: Item): Boolean = flagThieves.containsKey(player)

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

    private fun stealFlag(player: Player, flag: FlagStructure) {
        val team = warzone.teams[flag.kind] ?: return
        flagThieves[player] = flag

        // Fill the player's inventory with wool
        val contents = player.inventory.storageContents
        contents.forEachIndexed { i, _ ->
            contents[i] = ItemStack(flag.kind.material, 64)
        }
        player.inventory.storageContents = contents
        player.inventory.setItemInOffHand(null)

        player.clearPotionEffects()
        warzone.broadcast("${player.name} stole team $team's flag")
    }

    override fun saveConfig(config: ConfigurationSection) {
        val flags: MutableList<Map<String, String>> = mutableListOf()
        flagStructures.forEach {
            flags.add(
                mapOf(
                    "team" to it.kind.toString().toLowerCase(),
                    "origin" to it.origin.toString()
                )
            )
        }
        config.set("flags", flags)
    }

    override fun reset() {
        flagStructures.forEach {
            it.build()
        }
        flagThieves.clear()
    }
}