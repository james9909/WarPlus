package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.command.AbstractCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class DeleteTeamSpawnCommand(plugin: WarPlus, sender: CommandSender, args: List<String>) :
    AbstractCommand(plugin, sender, args) {
    override fun handle(): Boolean {
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
            team.spawns.forEach {
                if (it.contains(sender.location)) {
                    it.restore()
                    team.spawns.remove(it)
                    warzone.save()
                    plugin.playerManager.sendMessage(sender, "Spawn removed!")
                    return true
                }
            }
        }
        plugin.playerManager.sendMessage(sender, "There is no spawn at this location")
        return true
    }
}