package com.github.james9909.warplus.command.admin

import com.github.james9909.warplus.WARPLUS_BASE_COMMAND
import com.github.james9909.warplus.WarPlus
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import java.lang.NumberFormatException
import org.bukkit.command.CommandSender

class AdminStatsCommand : AdminCommand() {
    override val usageString = "/$WARPLUS_BASE_COMMAND adminstats <clear|addheal <player> <amount>>"
    override val description = "Manage warzone stats"

    override fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean {
        if (args.isEmpty()) {
            return false
        }
        when (args[0].toLowerCase()) {
            "clear" -> {
                plugin.server.scheduler.runTaskAsynchronously(plugin) { _ ->
                    when (plugin.databaseManager?.dropTables()) {
                        is Ok -> {
                            when (plugin.databaseManager?.createTables()) {
                                is Ok -> {
                                    plugin.playerManager.sendMessage(sender, "WarPlus stats cleared.")
                                }
                                is Err -> {
                                    plugin.playerManager.sendMessage(sender, "Recreate the database. Try again later.")
                                }
                            }
                        }
                        is Err -> {
                            plugin.playerManager.sendMessage(sender, "Failed to wipe the database. Try again later.")
                        }
                    }
                }
            }
            "addheal" -> {
                if (args.size < 3) {
                    showUsageString(plugin, sender)
                    return false
                }
                val player = plugin.server.getPlayer(args[1])
                if (player == null) {
                    plugin.playerManager.sendMessage(sender, "No such player ${args[1]}")
                    return true
                }
                val amount = try {
                    args[2].toInt()
                } catch (e: NumberFormatException) {
                    plugin.playerManager.sendMessage(sender, "Invalid amount ${args[2]}")
                    return true
                }
                val playerInfo = plugin.playerManager.getPlayerInfo(player)
                if (playerInfo == null) {
                    plugin.playerManager.sendMessage(sender, "${player.name} is not participating in a warzone.")
                    return true
                }
                val warzone = playerInfo.team.warzone
                plugin.server.scheduler.runTaskAsynchronously(plugin) addHealTask@{ _ ->
                    warzone.statTracker?.addHeal(player.uniqueId, amount)
                    plugin.playerManager.sendMessage(sender, "Heal added for ${player.name}.")
                }
            }
        }
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> {
                listOf("clear", "addheal").filter {
                    it.startsWith(args[0])
                }
            }
            else -> emptyList()
        }
    }
}
