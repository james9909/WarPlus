package com.github.james9909.warplus.command.regular

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.command.AbstractCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class LeaveWarzoneCommand(plugin: WarPlus, sender: CommandSender, args: List<String>) :
    AbstractCommand(plugin, sender, args) {
    override fun handle(): Boolean {
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that")
            return true
        }
        val playerInfo = plugin.playerManager.getPlayerInfo(sender)
        if (playerInfo == null) {
            plugin.playerManager.sendMessage(sender, "You are not in a warzone")
            return true
        }

        val warzone = playerInfo.team.warzone
        warzone.removePlayer(sender, playerInfo.team)
        return true
    }
}