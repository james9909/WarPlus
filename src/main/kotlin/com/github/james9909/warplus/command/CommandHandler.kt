package com.github.james9909.warplus.command

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.command.admin.LoadCommand
import com.github.james9909.warplus.command.admin.UnloadCommand
import com.github.james9909.warplus.command.regular.JoinWarzoneCommand
import com.github.james9909.warplus.command.zonemaker.CreateWarzoneCommand
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
            "create" -> CreateWarzoneCommand(plugin, sender, rest)
            "join" -> JoinWarzoneCommand(plugin, sender, rest)
            "load" -> LoadCommand(plugin, sender, rest)
            "unload" -> UnloadCommand(plugin, sender, rest)
            else -> null
        }
        return command
    }
}