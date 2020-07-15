package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.WARPLUS_BASE_COMMAND
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.command.AbstractCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class DeleteTeamSpawnCommand : AbstractCommand() {
    override val USAGE_STRING = "/$WARPLUS_BASE_COMMAND deleteteamspawn"
    override val DESCRIPTION = "Delete the spawn at your current location"

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
        warzone.teams.forEach { (_, team) ->
            team.spawns.forEach { spawn ->
                if (spawn.contains(sender.location)) {
                    spawn.restoreVolume()
                    warzone.removeTeamSpawn(spawn)
                    warzone.saveConfig()
                    plugin.playerManager.sendMessage(sender, "Spawn removed!")
                    return true
                }
            }
        }
        plugin.playerManager.sendMessage(sender, "There is no spawn at this location")
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        return emptyList()
    }
}