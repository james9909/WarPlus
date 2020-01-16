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

        for ((_, team) in playerInfo.team.warzone.teams) {
            for (flag in team.flagStructures) {
                if (flag.contains(block.location)) {
                    // Allow players to break only other team flags
                    event.isCancelled = block != flag.flagBlock || team.kind != playerInfo.team.kind
                    return
                }
            }
            for (spawn in team.spawns) {
                if (spawn.contains(block.location)) {
                    event.isCancelled = true
                    return
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val block = event.block
        val warzone = plugin.warzoneManager.getWarzoneByLocation(block.location) ?: return
        event.isCancelled = warzone.isStructureBlock(block)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockBurn(event: BlockBurnEvent) {
        val block = event.block
        val warzone = plugin.warzoneManager.getWarzoneByLocation(block.location) ?: return
        event.isCancelled = warzone.isStructureBlock(block)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockIgnite(event: BlockIgniteEvent) {
        val block = event.block
        val warzone = plugin.warzoneManager.getWarzoneByLocation(block.location) ?: return
        event.isCancelled = warzone.isStructureBlock(block)
    }
}