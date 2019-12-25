package com.github.james9909.warplus.command.admin

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.command.AbstractCommand
import com.github.james9909.warplus.util.Message
import org.bukkit.command.CommandSender

class LoadCommand(plugin: WarPlus, sender: CommandSender, args: List<String>) :
    AbstractCommand(plugin, sender, args) {

    override fun handle(): Boolean {
        if (plugin.loaded.get()) {
            plugin.playerManager.sendMessage(sender, Message.ALREADY_LOADED)
            return true
        }
        plugin.playerManager.sendMessage(sender, Message.LOADING_WAR_START)
        plugin.initialize()
        plugin.playerManager.sendMessage(sender, Message.LOADING_WAR_DONE)
        return true
    }
}
