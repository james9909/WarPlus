package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.WARPLUS_BASE_COMMAND
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.objectives.FlagObjective
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class DeleteTeamFlagCommand : ZonemakerCommand() {
    override val usageString = "/$WARPLUS_BASE_COMMAND deleteteamflag"
    override val description = "Delete the flag at your current location"

    override fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that")
            return true
        }
        val warzone = plugin.warzoneManager.getWarzoneByLocation(sender.location)
        if (warzone == null) {
            plugin.playerManager.sendMessage(sender, "You're not in a warzone")
            return true
        }
        val objective = warzone.objectives["flags"] as? FlagObjective
        if (objective == null) {
            plugin.playerManager.sendMessage(sender, "There are no flags in this warzone")
            return true
        }
        val flag = objective.getFlagAtLocation(sender.location)
        if (flag == null) {
            plugin.playerManager.sendMessage(sender, "There is no flag at this location")
            return true
        }
        flag.restoreVolume()
        objective.removeFlag(flag)
        warzone.saveConfig()
        plugin.playerManager.sendMessage(sender, "Flag removed!")
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        return emptyList()
    }
}
