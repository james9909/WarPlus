package com.github.james9909.warplus.command.regular

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.command.AbstractCommand
import com.github.james9909.warplus.util.Message
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

// Format: /join <warzone>
class JoinCommand(plugin: WarPlus, sender: CommandSender, args: List<String>) :
    AbstractCommand(plugin, sender, args) {

    override fun handle(): Boolean {
        if (args.isEmpty()) {
            return false
        }
        if (sender !is Player) {
            // Only players can join warzones
            return true
        }

        val warzone = plugin.warzoneManager.getWarzoneByName(args[0])
        if (warzone == null) {
            plugin.playerManager.sendMessage(sender, Message.NO_SUCH_WARZONE)
            return true
        }
        if (!warzone.enabled) {
            plugin.playerManager.sendMessage(sender, Message.WARZONE_DISABLED)
            return true
        }

        warzone.join(sender)
        return true
    }
}