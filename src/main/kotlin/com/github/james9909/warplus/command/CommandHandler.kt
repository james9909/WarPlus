package com.github.james9909.warplus.command

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.command.admin.LoadCommand
import com.github.james9909.warplus.command.admin.UnloadCommand
import com.github.james9909.warplus.command.regular.ClassCommand
import com.github.james9909.warplus.command.regular.JoinWarzoneCommand
import com.github.james9909.warplus.command.regular.LeaveWarzoneCommand
import com.github.james9909.warplus.command.zonemaker.AddMonumentCommand
import com.github.james9909.warplus.command.zonemaker.AddTeamFlagCommand
import com.github.james9909.warplus.command.zonemaker.AddTeamSpawnCommand
import com.github.james9909.warplus.command.zonemaker.ClassChestCommand
import com.github.james9909.warplus.command.zonemaker.CreateWarzoneCommand
import com.github.james9909.warplus.command.zonemaker.DeleteMonumentCommand
import com.github.james9909.warplus.command.zonemaker.DeleteTeamFlagCommand
import com.github.james9909.warplus.command.zonemaker.DeleteTeamSpawnCommand
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
            "addteamflag" -> AddTeamFlagCommand(plugin, sender, rest)
            "deleteteamflag" -> DeleteTeamFlagCommand(plugin, sender, rest)
            "addteamspawn" -> AddTeamSpawnCommand(plugin, sender, rest)
            "deleteteamspawn" -> DeleteTeamSpawnCommand(plugin, sender, rest)
            "classchest" -> ClassChestCommand(plugin, sender, rest)
            "class" -> ClassCommand(plugin, sender, rest)
            "leave" -> LeaveWarzoneCommand(plugin, sender, rest)
            "addmonument" -> AddMonumentCommand(plugin, sender, rest)
            "deletemonument" -> DeleteMonumentCommand(plugin, sender, rest)
            else -> null
        }
        return command
    }
}