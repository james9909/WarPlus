package com.github.james9909.warplus.command.admin

import com.github.james9909.warplus.command.WarCommand
import org.bukkit.command.CommandSender

abstract class AdminCommand : WarCommand {
    override fun canExecute(sender: CommandSender): Boolean {
        return sender.hasPermission("warplus.admin")
    }
}
