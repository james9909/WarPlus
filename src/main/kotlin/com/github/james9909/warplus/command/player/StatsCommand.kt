package com.github.james9909.warplus.command.player

import com.github.james9909.warplus.WARPLUS_BASE_COMMAND
import com.github.james9909.warplus.WarPlus
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class StatsCommand : PlayerCommand() {
    override val usageString = "/$WARPLUS_BASE_COMMAND stats"
    override val description = "View your stats"

    override fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that")
            return true
        }
        val dbm = plugin.databaseManager
        if (dbm == null) {
            plugin.playerManager.sendMessage(sender, "Stats are disabled.")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(plugin) { _ ->
            val stats = dbm.getPlayerStat(sender.uniqueId)
            if (stats == null) {
                plugin.playerManager.sendMessage(sender, "You have not completed a warzone yet.")
            } else {
                plugin.playerManager.sendMessage(sender, "Your stats:")
                plugin.playerManager.sendMessage(sender, "Wins: ${stats.wins}")
                plugin.playerManager.sendMessage(sender, "Losses: ${stats.losses}")
            }
        }
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        return emptyList()
    }
}
