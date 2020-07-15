package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WARPLUS_BASE_COMMAND
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.extensions.blockLocation
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AddTeamFlagCommand : ZonemakerCommand() {
    override val USAGE_STRING = "/$WARPLUS_BASE_COMMAND addteamflag <team>"
    override val DESCRIPTION = "Add a flag for a team"

    override fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean {
        if (args.isEmpty()) {
            return false
        }
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that")
            return true
        }
        val warzone = plugin.warzoneManager.getWarzoneByLocation(sender.location)
        if (warzone == null) {
            plugin.playerManager.sendMessage(sender, "You're not in a warzone")
            return true
        }
        val kind: TeamKind
        try {
            kind = TeamKind.valueOf(args[0].toUpperCase())
        } catch (e: IllegalArgumentException) {
            plugin.playerManager.sendMessage(sender, "Invalid team ${args[0]}")
            return true
        }
        val team = warzone.teams[kind]
        if (team == null) {
            plugin.playerManager.sendMessage(sender, "That team doesn't exist")
            return true
        }

        val origin = sender.location.subtract(0.0, 1.0, 0.0).blockLocation()
        val message = when (val result = warzone.addFlagObjective(origin, team.kind)) {
            is Ok -> "Flag for team ${args[0]} created!"
            is Err -> result.error.toString()
        }
        plugin.playerManager.sendMessage(sender, message)
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> TeamKind.values().map { it.name.toLowerCase() }
            else -> emptyList()
        }
    }
}