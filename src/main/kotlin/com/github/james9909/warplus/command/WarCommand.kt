package com.github.james9909.warplus.command

import com.github.james9909.warplus.WarPlus
import org.bukkit.command.CommandSender

interface WarCommand {
    val usageString: String
    val description: String

    fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean
    fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String>
    fun canExecute(sender: CommandSender): Boolean

    fun showUsageString(plugin: WarPlus, sender: CommandSender) {
        plugin.playerManager.sendMessage(sender, "Usage: ${usageString}\n$description")
    }
}
