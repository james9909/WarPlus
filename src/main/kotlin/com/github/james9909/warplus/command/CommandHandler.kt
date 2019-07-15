package com.github.james9909.warplus.command

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.command.zonemaker.SetupWarzoneCommand
import org.bukkit.command.CommandSender

class CommandHandler {

    fun getCommand(plugin: WarPlus, sender: CommandSender, args: Array<String>): AbstractCommand? {
        if (args.isEmpty()) {
            return null
        }

        val subCommand = args[0]
        val rest = args.drop(1)

        val command = when (subCommand) {
            "setup" -> SetupWarzoneCommand(plugin, sender, rest)
            else -> null
        }
        return command
    }
}