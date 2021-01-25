package com.github.james9909.warplus.command.player

import com.github.james9909.warplus.WARPLUS_BASE_COMMAND
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.managers.WarParticipant
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class LeaveWarzoneCommand : PlayerCommand() {
    override val usageString = "/$WARPLUS_BASE_COMMAND leave"
    override val description = "Leave the current warzone"

    override fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that")
            return true
        }
        val playerInfo = plugin.playerManager.getParticipantInfo(sender.uniqueId)
        if (playerInfo == null) {
            plugin.playerManager.sendMessage(sender, "You are not in a warzone")
            return true
        }

        when (playerInfo) {
            is WarParticipant.Spectator -> playerInfo.warzone.removeSpectator(sender)
            is WarParticipant.Player -> {
                val warzone = playerInfo.team.warzone
                warzone.removePlayer(sender, playerInfo.team)
            }
        }
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        return emptyList()
    }
}
