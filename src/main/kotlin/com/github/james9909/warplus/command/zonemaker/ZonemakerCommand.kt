package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.command.AbstractCommand
import org.bukkit.command.CommandSender

abstract class ZonemakerCommand : AbstractCommand() {
    override fun canExecute(sender: CommandSender): Boolean {
        return sender.hasPermission("warplus.zonemaker")
    }
}