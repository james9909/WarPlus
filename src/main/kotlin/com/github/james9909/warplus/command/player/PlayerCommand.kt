package com.github.james9909.warplus.command.player

import com.github.james9909.warplus.command.AbstractCommand
import org.bukkit.command.CommandSender

abstract class PlayerCommand : AbstractCommand() {
    override fun canExecute(sender: CommandSender): Boolean {
        return sender.hasPermission("warplus.player")
    }
}