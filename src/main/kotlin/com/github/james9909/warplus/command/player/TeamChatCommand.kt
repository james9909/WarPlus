package com.github.james9909.warplus.command.player

import com.github.james9909.warplus.WARPLUS_BASE_COMMAND
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.managers.WarParticipant
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class TeamChatCommand : PlayerCommand() {
    override val usageString = "/$WARPLUS_BASE_COMMAND <team> <message>"
    override val description = "Send a message to your team"

    override fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that")
            return true
        }
        val playerInfo = plugin.playerManager.getPlayerInfo(sender)
        if (playerInfo == null) {
            plugin.playerManager.sendMessage(sender, "You are not in a warzone")
            return true
        }

        val team = playerInfo.team
        playerInfo.team.broadcast("${team.kind.chatColor}${sender.name}${ChatColor.RESET}: ${args.joinToString(" ")}${ChatColor.RESET}")
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        return emptyList()
    }
}
