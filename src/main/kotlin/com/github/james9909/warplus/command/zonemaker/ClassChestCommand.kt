package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.ArmorSet
import com.github.james9909.warplus.WarClass
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.command.AbstractCommand
import org.bukkit.block.Chest
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ClassChestCommand : AbstractCommand() {
    override val USAGE_STRING = "/war classchest <set|remove> <name>"
    override val DESCRIPTION = "Set or remove a classchest"

    override fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean {
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
                val targetBlock = sender.getTargetBlock(null, 10)
                val state = targetBlock.state
                if (state !is Chest) {
                    plugin.playerManager.sendMessage(sender, "You are not looking at a chest")
                    return true
                }
                val warClass = WarClass(className, null, mutableMapOf(), ArmorSet.default(), targetBlock.location)
                plugin.classManager.addClass(className, warClass)
                plugin.classManager.saveConfig()
                plugin.playerManager.sendMessage(sender, "Class $className set!")
            }
            "remove" -> {
                val warClass = plugin.classManager.getClass(className)
                val message = if (warClass != null) {
                    warClass.classchest = null
                    plugin.classManager.saveConfig()
                    "Classchest for $className has been updated"
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