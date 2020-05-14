package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.command.AbstractCommand
import com.github.james9909.warplus.extensions.blockLocation
import com.github.james9909.warplus.structures.FlagStructure
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AddTeamFlagCommand(plugin: WarPlus, sender: CommandSender, args: List<String>) : AbstractCommand(plugin, sender, args) {
    override fun handle(): Boolean {
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

        val message = if (warzone.addFlagObjective(sender.location.subtract(0.0, 1.0, 0.0).blockLocation(), team.kind)) {
            "Flag for team ${args[0]} created!"
        } else {
            "Flags cannot overlap with any other structures"
        }
        plugin.playerManager.sendMessage(sender, message)
        return true
    }
}