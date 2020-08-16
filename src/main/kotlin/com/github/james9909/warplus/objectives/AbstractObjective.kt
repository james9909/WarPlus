package com.github.james9909.warplus.objectives

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction

abstract class AbstractObjective(plugin: WarPlus, warzone: Warzone) {
    abstract val name: String

    open fun handleBlockBreak(player: Player?, block: Block): Boolean {
        return false
    }

    open fun handleBlockPlace(entity: Entity, block: Block): Boolean {
        return false
    }

    open fun handleItemPickup(player: Player, item: Item): Boolean {
        return false
    }

    open fun handleInventoryClick(player: Player, action: InventoryAction): Boolean {
        return false
    }

    open fun handlePlayerDropItem(player: Player, item: Item): Boolean {
        return false
    }

    open fun handleSpellCast(player: Player): Boolean {
        return false
    }

    open fun handlePlayerMove(player: Player, from: Location, to: Location) {}

    open fun handleDeath(player: Player) {}

    open fun handleLeave(player: Player) {}

    abstract fun saveConfig(config: ConfigurationSection)

    abstract fun reset()

    abstract fun delete()
}