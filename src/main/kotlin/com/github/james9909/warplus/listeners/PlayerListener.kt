package com.github.james9909.warplus.listeners

import com.github.james9909.warplus.WarPlus
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent

class PlayerListener(val plugin: WarPlus) : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return

        val warzone = playerInfo.team.warzone
        warzone.removePlayer(player, playerInfo.team)
        plugin.playerManager.removePlayer(player)
        playerInfo.state.restore(player)
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
}