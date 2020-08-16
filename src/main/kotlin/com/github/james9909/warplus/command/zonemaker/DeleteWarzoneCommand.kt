package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.WARPLUS_BASE_COMMAND
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.WarzoneState
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class DeleteWarzoneCommand: ZonemakerCommand() {
    override val USAGE_STRING = "/$WARPLUS_BASE_COMMAND deletezone <warzone name>"
    override val DESCRIPTION = "Fully delete a warzone"

    override fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean {
        if (args.isEmpty()) {
            return false
        }
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that")
            return true
        }
        val warzone = plugin.warzoneManager.getWarzone(args[0])
        if (warzone == null) {
            plugin.playerManager.sendMessage(sender, "That warzone doesn't exist")
            return true
        }
        if (warzone.state != WarzoneState.IDLING) {
            plugin.playerManager.sendMessage(sender, "That warzone is currently busy")
            return true
        }
        plugin.warzoneManager.deleteWarzone(warzone)
        plugin.playerManager.sendMessage(sender, "Warzone ${warzone.name} deleted!")
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> plugin.warzoneManager.getWarzoneNames().filter {
                it.startsWith(args[0].toLowerCase())
            }
            else -> emptyList()
        }
    }
}