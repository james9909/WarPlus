package com.github.james9909.warplus.command.admin

import com.github.james9909.warplus.command.AbstractCommand
import org.bukkit.command.CommandSender

abstract class AdminCommand : AbstractCommand() {
    override fun canExecute(sender: CommandSender): Boolean {
        return sender.hasPermission("warplus.admin")
    }
}