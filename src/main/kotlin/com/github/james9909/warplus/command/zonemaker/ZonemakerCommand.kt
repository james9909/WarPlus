package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.command.WarCommand
import org.bukkit.command.CommandSender

abstract class ZonemakerCommand : WarCommand {
    override fun canExecute(sender: CommandSender): Boolean {
        return sender.hasPermission("warplus.zonemaker")
    }
}
