package com.github.james9909.warplus.listeners

import com.github.james9909.warplus.WarPlus
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent

class PlayerListener(val plugin: WarPlus) : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return

        val warzone = playerInfo.team.warzone
        warzone.removePlayer(player, playerInfo.team)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val player = event.player
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return

        if (event.cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            event.isCancelled = true
            return
        }
        if (event.cause == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            event.isCancelled = true
            return
        }

        val to = event.to ?: return
        if (!playerInfo.team.warzone.contains(to)) {
            // Prevent teleporting outside of warzones
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerDamageItem(event: PlayerItemDamageEvent) {
        val player = event.player
        plugin.playerManager.getPlayerInfo(player) ?: return

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val to = event.to ?: return
        val from = event.from
        if (to.blockX == from.blockX && to.blockY == from.blockY && to.blockZ == from.blockZ) {
            // Player hasn't moved to a different block
            return
        }

        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
        val team = playerInfo.team
        val inSpawn = playerInfo.team.spawns.any {
            it.contains(to)
        }
        if (playerInfo.inSpawn) {
            if (!inSpawn) {
                // Player has exited the spawn
                playerInfo.inSpawn = false
            }
            return
        }
        if (team.warzone.flagThieves.containsKey(player)) {
            if (inSpawn) {
                val flag = team.warzone.flagThieves[player] ?: return // Null case should never happen
                flag.build()
                team.warzone.broadcast("${player.name} captured ${flag.kind.format()}'s flag. Team $team scores one point.")
                team.addPoint()

                // Detect win condition
                if (team.score >= team.settings.getInt("max-score", 2)) {
                    team.warzone.handleWin(listOf(team))
                    return
                }
                team.warzone.removeFlagThief(player)
                team.warzone.respawnPlayer(player)
            }
        }
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
        event.isCancelled = true
    }

    @EventHandler
    fun onInventoryClickEvent(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
        if (playerInfo.team.warzone.flagThieves.containsKey(player)) {
            event.isCancelled = true
        }
    }
}