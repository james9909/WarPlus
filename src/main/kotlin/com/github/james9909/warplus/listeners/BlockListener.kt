package com.github.james9909.warplus.listeners

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.WarzoneState
import com.github.james9909.warplus.config.TeamConfigType
import com.github.james9909.warplus.config.WarzoneConfigType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.inventory.ItemStack

class BlockListener(val plugin: WarPlus) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        val player = event.player
        val targetZone = plugin.warzoneManager.getWarzoneByLocation(block.location)
        val playerInfo = plugin.playerManager.getPlayerInfo(player)
        if (playerInfo == null) {
            if (targetZone != null && targetZone.state != WarzoneState.EDITING) {
                event.isCancelled = true
            }
            return
        }
        if (playerInfo.inSpawn) {
            event.isCancelled = true
            return
        }

        val playerZone = playerInfo.team.warzone
        if (!playerZone.warzoneSettings.get(WarzoneConfigType.CAN_BREAK_BLOCKS)) {
            event.isCancelled = true
            return
        }
        if (playerZone != targetZone ||
            targetZone.state != WarzoneState.RUNNING ||
            playerZone.isSpawnBlock(block)
        ) {
            // In-game players cannot break blocks outside of their warzone or spawn blocks
            event.isCancelled = true
            return
        }
        event.isCancelled = playerZone.onBlockBreak(player, block)
        if (!event.isCancelled) {
            val toDrop = ItemStack(event.block.type)
            if (plugin.itemNameManager.applyItem(toDrop)) {
                block.world.dropItemNaturally(block.location, toDrop)
                event.isDropItems = false
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val block = event.block
        val player = event.player
        val targetZone = plugin.warzoneManager.getWarzoneByLocation(block.location)
        val playerInfo = plugin.playerManager.getPlayerInfo(player)
        if (playerInfo == null) {
            if (targetZone != null && targetZone.state != WarzoneState.EDITING) {
                event.isCancelled = true
            }
            return
        }

        if (playerInfo.inSpawn) {
            event.isCancelled = true
            return
        }

        if (targetZone == null ||
                targetZone != playerInfo.team.warzone ||
                targetZone.isSpawnBlock(block) ||
                targetZone.state != WarzoneState.RUNNING ||
                !playerInfo.team.settings.get(TeamConfigType.PLACE_BLOCKS)) {
            event.isCancelled = true
            return
        }
        event.isCancelled = targetZone.onBlockPlace(player, block)
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

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockSpread(event: BlockSpreadEvent) {
        val block = event.block
        val warzone = plugin.warzoneManager.getWarzoneByLocation(block.location) ?: return
        event.isCancelled = warzone.isSpawnBlock(block) || warzone.onBlockBreak(null, block)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockFromTo(event: BlockFromToEvent) {
        val to = event.toBlock
        val warzone = plugin.warzoneManager.getWarzoneByLocation(to.location) ?: return
        event.isCancelled = warzone.isSpawnBlock(to) || warzone.onBlockBreak(null, to)
    }
}
