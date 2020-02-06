package com.github.james9909.warplus.listeners

import com.github.james9909.warplus.WarPlus
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

        val warzone = playerInfo.team.warzone
        if (warzone.isSpawnBlock(block)) {
            event.isCancelled = true
            return
        }
        event.isCancelled = warzone.onBlockBreak(player, block)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val block = event.block
        val warzone = plugin.warzoneManager.getWarzoneByLocation(block.location) ?: return
        if (warzone.isSpawnBlock(block)) {
            event.isCancelled = true
            return
        }
        val player = event.player
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
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