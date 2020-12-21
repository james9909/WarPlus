package com.github.james9909.warplus.managers

import com.github.james9909.warplus.WarPlus
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.io.File
import java.lang.ClassCastException

class InventoryManager(val plugin: WarPlus) {
    private val baseDirectory = File(plugin.dataFolder, "inventories")
    private val players = HashMap<Player, Array<ItemStack?>>()

    private fun getPlayerFile(player: Player): File = File(baseDirectory, "${player.name}.yml")

    fun saveInventory(player: Player) {
        val inventoryContents = player.inventory.contents
        players[player] = inventoryContents
        val playerFile = getPlayerFile(player)
        val config = YamlConfiguration()
        config.set("inventory", inventoryContents)
        config.save(playerFile)
    }

    fun restoreInventory(player: Player) {
        val items = players[player]
        if (items == null) {
            // Restore from file
            restoreInventoryFromFile(player)
            return
        }
        player.inventory.contents = items
        players.remove(player)

        // Delete file on disk to avoid accidental restorations
        val playerFile = getPlayerFile(player)
        if (playerFile.exists()) {
            playerFile.delete()
        }
    }

    fun restoreInventoryFromFile(player: Player) {
        val playerFile = getPlayerFile(player)
        if (!playerFile.exists()) {
            return
        }
        val config = YamlConfiguration.loadConfiguration(playerFile)
        val items = config.getList("inventory") ?: return
        val newItems = arrayOfNulls<ItemStack?>(items.size)
        items.forEachIndexed { index, item ->
            try {
                newItems[index] = item as? ItemStack
            } catch (e: ClassCastException) {
                plugin.logger.warning("Failed to restore item: $item")
            }
        }
        player.inventory.contents = newItems
        playerFile.delete()
    }
}
