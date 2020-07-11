package com.github.james9909.warplus.listeners

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.config.TeamConfigType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockPlaceEvent

class BlockListener(val plugin: WarPlus) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        val player = event.player
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
        if (playerInfo.inSpawn) {
            event.isCancelled = true
            return
        }

        val playerZone = playerInfo.team.warzone
        val targetZone = plugin.warzoneManager.getWarzoneByLocation(block.location)
        if (playerZone != targetZone || playerZone.isSpawnBlock(block)) {
            // In-game players cannot break blocks outside of their warzone or spawn blocks
            event.isCancelled = true
            return
        }
        event.isCancelled = playerZone.onBlockBreak(player, block)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
        if (playerInfo.inSpawn) {
            event.isCancelled = true
            return
        }

        val block = event.block
        val warzone = plugin.warzoneManager.getWarzoneByLocation(block.location)
        if (warzone == null ||
                warzone != playerInfo.team.warzone ||
                warzone.isSpawnBlock(block) ||
                !playerInfo.team.settings.get(TeamConfigType.PLACE_BLOCKS)) {
            event.isCancelled = true
            return
        }
        event.isCancelled = warzone.onBlockPlace(player, block)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockBurn(event: BlockBurnEvent) {
        val block = event.block
        val warzone = plugin.warzoneManager.getWarzoneByLocation(block.location) ?: return
        event.isCancelled = warzone.onBlockBreak(null, block)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockIgnite(event: BlockIgniteEvent) {
        val block = event.block
        val warzone = plugin.warzoneManager.getWarzoneByLocation(block.location) ?: return
        event.isCancelled = warzone.onBlockBreak(null, block)
    }
}