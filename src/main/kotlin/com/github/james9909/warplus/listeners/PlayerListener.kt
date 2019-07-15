package com.github.james9909.warplus.listeners

import com.github.james9909.warplus.WarPlus
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener(val plugin: WarPlus) : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
        if (playerInfo.team == null) {
            return
        }
        val warzone = playerInfo.team.warzone
        warzone.removePlayer(player, playerInfo.team)
        plugin.playerManager.removePlayer(player)
    }
}