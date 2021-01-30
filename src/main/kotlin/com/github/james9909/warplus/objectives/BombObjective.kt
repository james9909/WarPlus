package com.github.james9909.warplus.objectives

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.config.TeamConfigType
import com.github.james9909.warplus.extensions.clearPotionEffects
import com.github.james9909.warplus.extensions.format
import com.github.james9909.warplus.extensions.toLocation
import com.github.james9909.warplus.structures.BombStructure
import java.util.UUID
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.ItemStack

fun createBombObjective(plugin: WarPlus, warzone: Warzone, config: ConfigurationSection): BombObjective? {
    val bombs = config.getMapList("locations").map { cpMap ->
        val name = cpMap["name"] as String
        val origin = (cpMap["origin"] as String).toLocation()
        BombStructure(plugin, origin, name)
    }
    return BombObjective(
        plugin, warzone, bombs.toMutableList()
    )
}

class BombObjective(
    private val plugin: WarPlus,
    private val warzone: Warzone,
    val bombs: MutableList<BombStructure>
) : Objective(plugin, warzone) {
    override val name: String = "bombs"
    private val bombCarriers = hashMapOf<UUID, BombStructure>()

    fun addBomb(bomb: BombStructure) = bombs.add(bomb)

    fun removeBomb(bomb: BombStructure) = bombs.remove(bomb)

    fun getBombAtLocation(location: Location): BombStructure? = bombs.firstOrNull { it.contains(location) }

    override fun handleBlockBreak(player: Player?, block: Block): Boolean {
        val bomb = bombs.firstOrNull { it.contains(block.location) } ?: return false
        if (player == null) return true
        if (block != bomb.tntBlock) return true

        val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId) ?: return true
        if (playerInfo.team.warzone != warzone) return true
        if (bombCarriers.containsKey(player.uniqueId)) return true

        bomb.tntBlock.type = Material.AIR
        bombCarriers[player.uniqueId] = bomb

        // Fill the player's inventory with tnt
        val contents = player.inventory.storageContents
        contents.forEachIndexed { i, _ ->
            contents[i] = ItemStack(Material.TNT, 64)
        }
        player.inventory.storageContents = contents
        player.inventory.setItemInOffHand(null)

        player.clearPotionEffects()
        warzone.broadcast("${player.name} picked up bomb ${bomb.name}.")
        return false
    }

    override fun handleBlockPlace(entity: Entity?, block: Block): Boolean {
        if (entity is Player && bombCarriers.containsKey(entity.uniqueId)) {
            return true
        }
        return bombs.any { it.contains(block.location) }
    }

    override fun handleItemPickup(player: Player, item: Item): Boolean = bombCarriers.containsKey(player.uniqueId)

    override fun handlePlayerMove(player: Player, from: Location, to: Location) {
        val bomb = bombCarriers[player.uniqueId] ?: return
        val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId) ?: return
        val playerTeam = playerInfo.team
        val victimTeam = playerTeam.warzone.teams
            .values
            .firstOrNull { team ->
                team.kind != playerTeam.kind && team.spawns.any { spawn -> spawn.contains(to) }
            } ?: return

        if (!victimTeam.hasEnoughPlayers()) {
            plugin.playerManager.sendMessage(
                player,
                "You can't blow up $victimTeam's spawn since there are not enough players on that team!"
            )
        } else {
            // Blow up the spawn
            playerTeam.warzone.broadcast("${player.name} blew up $victimTeam's spawn. Team $playerTeam scores one point.")
            player.world.spawnParticle(Particle.EXPLOSION_HUGE, player.location, 1)
            playerTeam.addPoint()
            bomb.build()
            bombCarriers.remove(player.uniqueId)

            if (playerTeam.score >= playerTeam.settings.get(TeamConfigType.MAX_SCORE)) {
                playerTeam.warzone.handleWin(listOf(playerTeam.kind))
            } else {
                playerTeam.warzone.respawnPlayer(player)
            }
        }
    }

    override fun handlePlayerDropItem(player: Player, item: Item): Boolean = bombCarriers.containsKey(player.uniqueId)

    override fun handleSpellCast(player: Player): Boolean = bombCarriers.containsKey(player.uniqueId)

    override fun handleInventoryClick(player: Player, action: InventoryAction): Boolean =
        bombCarriers.containsKey(player.uniqueId)

    override fun handleDeath(player: Player) = dropBomb(player)

    override fun handleLeave(player: Player) = dropBomb(player)

    private fun dropBomb(player: Player) {
        if (!bombCarriers.containsKey(player.uniqueId)) {
            return
        }
        val bomb = bombCarriers.remove(player.uniqueId) ?: return
        bomb.build()
        warzone.broadcast("${player.name} has dropped bomb ${bomb.name}")
    }

    override fun saveConfig(config: ConfigurationSection) {
        config.set("locations", bombs.map {
            mapOf(
                "name" to it.name,
                "origin" to it.origin.format()
            )
        })
    }

    override fun reset() {
        bombs.forEach { it.build() }
        bombCarriers.clear()
    }

    override fun delete() {
        bombs.forEach { it.deleteVolume() }
    }
}
