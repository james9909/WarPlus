package com.github.james9909.warplus.listeners

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.WarzoneState
import com.github.james9909.warplus.config.WarzoneConfigType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.ItemStack

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

        val playerInfo = plugin.playerManager.getPlayerInfo(player)
        if (playerInfo == null) {
            // Handle player movement outside of warzones
            plugin.warzoneManager.getWarzones().forEach { warzone ->
                if (!warzone.isEnabled() || warzone.state == WarzoneState.EDITING) {
                    return@forEach
                }
                warzone.getPortalByLocation(to) ?: return@forEach
                warzone.addPlayer(player)
            }
            return
        }

        val team = playerInfo.team
        val inSpawn = team.spawns.any {
            it.contains(to)
        }
        if (playerInfo.inSpawn) {
            if (!inSpawn) {
                // Player has exited the spawn
                val warzone = team.warzone
                if (warzone.state != WarzoneState.RUNNING) {
                    // Players cannot leave if the warzone has not started yet
                    plugin.playerManager.sendMessage(player, "You cannot leave until the warzone has started")
                    event.isCancelled = true
                    return
                }
                playerInfo.inSpawn = false
            }
            return
        }
        team.warzone.onPlayerMove(player, from, to)
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
        val warzone = playerInfo.team.warzone
        if (!warzone.warzoneSettings.get(WarzoneConfigType.ITEM_DROPS) || playerInfo.inSpawn) {
            event.isCancelled = true
            return
        }
        event.isCancelled = warzone.onPlayerDropItem(player, event.itemDrop)
    }

    @EventHandler
    fun onInventoryClickEvent(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
        val teamMaterial = playerInfo.team.kind.material
        if (event.slotType == InventoryType.SlotType.ARMOR && event.currentItem?.type == teamMaterial) {
            if (event.isRightClick) {
                event.whoClicked.setItemOnCursor(ItemStack(teamMaterial))
            }
            event.isCancelled = true
            return
        }
        event.isCancelled = playerInfo.team.warzone.onInventoryClick(player, event.action)
    }

    @EventHandler
    fun onPlayerToggleSneakEvent(event: PlayerToggleSneakEvent) {
        if (!event.isSneaking) {
            // Only handle the initial sneak, not release
            return
        }

        val player = event.player
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
        if (!playerInfo.inSpawn) {
            plugin.playerManager.sendMessage(player, "Can't change class after exiting the spawn.")
            return
        }

        val warzone = playerInfo.team.warzone
        val classCmd = warzone.warzoneSettings.get(WarzoneConfigType.CLASS_CMD)
        if (classCmd != "") {
            // Execute custom class-choosing command
            player.performCommand(classCmd)
            return
        }

        val classes = playerInfo.team.resolveClasses()
        if (classes.isEmpty()) {
            return
        }
        val currentClass = playerInfo.warClass ?: return
        val idx = classes.indexOf(currentClass.name)
        val nextClassName = if (idx + 1 < classes.size) classes[idx + 1] else classes[0]
        val nextClass = plugin.classManager.getClass(nextClassName) ?: return
        playerInfo.team.warzone.equipClass(player, nextClass, true)
        plugin.playerManager.sendMessage(player, "Equipped $nextClassName class")
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        plugin.inventoryManager.restoreInventoryFromFile(event.player)
    }

    @EventHandler
    fun onPlayerBucketEmpty(event: PlayerBucketEmptyEvent) {
        val block = event.blockClicked
        val warzone = plugin.warzoneManager.getWarzoneByLocation(block.location) ?: return
        event.isCancelled = warzone.isSpawnBlock(block) || warzone.onBlockPlace(event.player, block)
    }
}