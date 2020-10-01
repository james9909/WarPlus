package com.github.james9909.warplus.command.player

import com.github.james9909.warplus.command.WarCommand
import org.bukkit.command.CommandSender

abstract class PlayerCommand : WarCommand {
    override fun canExecute(sender: CommandSender): Boolean {
        return sender.hasPermission("warplus.player")
    }
}
