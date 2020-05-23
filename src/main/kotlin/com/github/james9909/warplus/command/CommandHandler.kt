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
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class CommandHandler(val plugin: WarPlus) : CommandExecutor, TabCompleter {
    private val COMMANDS: MutableMap<String, AbstractCommand> = mutableMapOf()

    init {
        COMMANDS["setup"] = SetupWarzoneCommand()
        COMMANDS["create"] = CreateWarzoneCommand()
        COMMANDS["join"] = JoinWarzoneCommand()
        COMMANDS["leave"] = LeaveWarzoneCommand()
        COMMANDS["load"] = LoadCommand()
        COMMANDS["unload"] = UnloadCommand()
        COMMANDS["addteamflag"] = AddTeamFlagCommand()
        COMMANDS["deleteteamflag"] = DeleteTeamFlagCommand()
        COMMANDS["addteamspawn"] = AddTeamSpawnCommand()
        COMMANDS["deleteteamspawn"] = DeleteTeamSpawnCommand()
        COMMANDS["classchest"] = ClassChestCommand()
        COMMANDS["class"] = ClassCommand()
        COMMANDS["addmonument"] = AddMonumentCommand()
        COMMANDS["deletemonument"] = DeleteMonumentCommand()
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (sender !is Player) {
            return mutableListOf()
        }
        if (sender.isConversing) {
            return mutableListOf()
        }
        return mutableListOf()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            return false
        }
        val subCommand = args[0]
        val warCommand = COMMANDS[subCommand] ?: return false
        val rest = args.drop(1)
        return warCommand.execute(plugin, sender, rest)
    }
}