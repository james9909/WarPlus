package com.github.james9909.warplus.command

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.command.admin.AdminStatsCommand
import com.github.james9909.warplus.command.admin.LoadCommand
import com.github.james9909.warplus.command.admin.ReloadCommand
import com.github.james9909.warplus.command.admin.UnloadCommand
import com.github.james9909.warplus.command.player.ClassCommand
import com.github.james9909.warplus.command.player.JoinWarzoneCommand
import com.github.james9909.warplus.command.player.LeaveWarzoneCommand
import com.github.james9909.warplus.command.player.SpectateWarzoneCommand
import com.github.james9909.warplus.command.zonemaker.AddCapturePointCommand
import com.github.james9909.warplus.command.zonemaker.AddMonumentCommand
import com.github.james9909.warplus.command.zonemaker.AddPortalCommand
import com.github.james9909.warplus.command.zonemaker.AddTeamFlagCommand
import com.github.james9909.warplus.command.zonemaker.AddTeamSpawnCommand
import com.github.james9909.warplus.command.zonemaker.ClassChestCommand
import com.github.james9909.warplus.command.zonemaker.CreateWarzoneCommand
import com.github.james9909.warplus.command.zonemaker.DeleteCapturePointCommand
import com.github.james9909.warplus.command.zonemaker.DeleteMonumentCommand
import com.github.james9909.warplus.command.zonemaker.DeletePortalCommand
import com.github.james9909.warplus.command.zonemaker.DeleteTeamFlagCommand
import com.github.james9909.warplus.command.zonemaker.DeleteTeamSpawnCommand
import com.github.james9909.warplus.command.zonemaker.DeleteWarzoneCommand
import com.github.james9909.warplus.command.zonemaker.RewardsCommand
import com.github.james9909.warplus.command.zonemaker.SetupWarzoneCommand
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.UUID

class CommandHandler(val plugin: WarPlus) : CommandExecutor, TabCompleter {
    private val commands: MutableMap<String, WarCommand> = mutableMapOf()

    init {
        commands["setup"] = SetupWarzoneCommand()
        commands["create"] = CreateWarzoneCommand()
        commands["join"] = JoinWarzoneCommand()
        commands["leave"] = LeaveWarzoneCommand()
        commands["load"] = LoadCommand()
        commands["unload"] = UnloadCommand()
        commands["addteamflag"] = AddTeamFlagCommand()
        commands["deleteteamflag"] = DeleteTeamFlagCommand()
        commands["addteamspawn"] = AddTeamSpawnCommand()
        commands["deleteteamspawn"] = DeleteTeamSpawnCommand()
        commands["classchest"] = ClassChestCommand()
        commands["class"] = ClassCommand()
        commands["addmonument"] = AddMonumentCommand()
        commands["deletemonument"] = DeleteMonumentCommand()
        commands["reload"] = ReloadCommand()
        commands["addportal"] = AddPortalCommand()
        commands["deleteportal"] = DeletePortalCommand()
        commands["deletezone"] = DeleteWarzoneCommand()
        commands["spectate"] = SpectateWarzoneCommand()
        commands["addcapturepoint"] = AddCapturePointCommand()
        commands["deletecapturepoint"] = DeleteCapturePointCommand()
        commands["rewards"] = RewardsCommand()
        commands["adminstats"] = AdminStatsCommand()
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (sender !is Player || sender.isConversing) {
            return mutableListOf()
        }
        if (args.isEmpty()) {
            return commands
                .filter { it.value.canExecute(sender) }
                .map { it.key }
                .toMutableList()
        }
        if (args.size == 1) {
            // We only have the base subcommand
            return commands
                .filter { it.key.startsWith(args[0]) && it.value.canExecute(sender) }
                .map { it.key }
                .toMutableList()
        }
        val command = commands[args[0]] ?: return mutableListOf()
        return if (!command.canExecute(sender)) {
            mutableListOf()
        } else {
            command.tab(plugin, sender, args.drop(1)).toMutableList()
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            return false
        }
        val subCommand = args[0]
        val warCommand = commands[subCommand] ?: return false
        val rest = args.drop(1)
        if (!warCommand.canExecute(sender)) {
            plugin.playerManager.sendMessage(sender, "You don't have permission to execute that command")
            return true
        }
        try {
            if (!warCommand.execute(plugin, sender, rest)) {
                warCommand.showUsageString(plugin, sender)
            }
        } catch (e: Exception) {
            val uuid = UUID.randomUUID()
            plugin.logger.severe("Exception generated while executing command: $uuid")
            e.printStackTrace()
            plugin.playerManager.sendMessage(sender, "${ChatColor.RED}An internal plugin error occurred. Please submit a report on #bug-reports with the id $uuid${ChatColor.RESET}")
        }
        return true
    }
}
