package com.github.james9909.warplus.command.admin

import com.github.james9909.warplus.WARPLUS_BASE_COMMAND
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.util.Message
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.bukkit.command.CommandSender

class ReloadCommand : AdminCommand() {
    override val usageString = "/$WARPLUS_BASE_COMMAND reload"
    override val description = "Unload and reload the plugin"

    private fun resetPlugin(plugin: WarPlus, sender: CommandSender) {
        if (!plugin.loaded.get()) {
            plugin.playerManager.sendMessage(sender, Message.ALREADY_UNLOADED)
            return
        }
        plugin.playerManager.sendMessage(sender, Message.UNLOADING_WAR_START)
        plugin.disable()
        plugin.playerManager.sendMessage(sender, Message.UNLOADING_WAR_DONE)
        plugin.playerManager.sendMessage(sender, Message.LOADING_WAR_START)
        plugin.initialize()
        plugin.playerManager.sendMessage(sender, Message.LOADING_WAR_DONE)
    }

    private fun resetWarzone(plugin: WarPlus, sender: CommandSender, warzoneName: String) {
        if (!plugin.warzoneManager.unloadWarzone(warzoneName)) {
            plugin.playerManager.sendMessage(sender, "Warzone $warzoneName does not exist.")
        } else {
            plugin.playerManager.sendMessage(sender, "Warzone unloaded.")
            when (val warzoneResult = plugin.warzoneManager.loadWarzone(warzoneName)) {
                is Ok -> {
                    plugin.playerManager.sendMessage(sender, "Warzone loaded.")
                    plugin.warzoneManager.addWarzone(warzoneResult.value)
                }
                is Err -> plugin.playerManager.sendMessage(sender, "Error when loading warzone: ${warzoneResult.error}")
            }
        }
    }

    override fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean {
        if (args.isEmpty()) {
            resetPlugin(plugin, sender)
        } else {
            resetWarzone(plugin, sender, args[0])
        }
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        return plugin.warzoneManager.getWarzoneNames().filter {
            it.startsWith(args[0].toLowerCase())
        }
    }
}
