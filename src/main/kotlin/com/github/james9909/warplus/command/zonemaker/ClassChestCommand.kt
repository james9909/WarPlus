package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.WarClass
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.command.AbstractCommand
import org.bukkit.block.Chest
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ClassChestCommand(plugin: WarPlus, sender: CommandSender, args: List<String>) :
    AbstractCommand(plugin, sender, args) {
    override fun handle(): Boolean {
        if (args.size != 2) {
            return false
        }
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that")
            return true
        }
        val action = args[0]
        val className = args[1]
        when (action) {
            "set" -> {
                val state = sender.getTargetBlock(null, 10).state
                if (state !is Chest) {
                    plugin.playerManager.sendMessage(sender, "You are not looking at a chest")
                    return true
                }
                val inventory = state.blockInventory
                val warClass = WarClass.fromInventory(className, inventory)
                plugin.classManager.addClass(className, warClass)
                plugin.classManager.saveConfig()
                plugin.playerManager.sendMessage(sender, "Class $className set!")
            }
            "remove" -> {
                val message = if (plugin.classManager.removeClass(className)) {
                    plugin.classManager.saveConfig()
                    "Class $className removed!"
                } else {
                    "Class $className does not exist"
                }
                plugin.playerManager.sendMessage(sender, message)
            }
            else -> plugin.playerManager.sendMessage(sender, "Unknown action $action. Please try 'set' or 'remove'")
        }
        return true
    }
}