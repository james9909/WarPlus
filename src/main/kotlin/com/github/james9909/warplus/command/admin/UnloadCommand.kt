package com.github.james9909.warplus.command.admin

import com.github.james9909.warplus.WARPLUS_BASE_COMMAND
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.util.Message
import org.bukkit.command.CommandSender

class UnloadCommand : AdminCommand() {
    override val usageString = "/$WARPLUS_BASE_COMMAND unload"
    override val description = "Unload the plugin."

    override fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean {
        if (!plugin.loaded.get()) {
            plugin.playerManager.sendMessage(sender, Message.ALREADY_UNLOADED)
            return true
        }
        plugin.playerManager.sendMessage(sender, Message.UNLOADING_WAR_START)
        plugin.disable()
        plugin.playerManager.sendMessage(sender, Message.UNLOADING_WAR_DONE)
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        return emptyList()
    }
}
