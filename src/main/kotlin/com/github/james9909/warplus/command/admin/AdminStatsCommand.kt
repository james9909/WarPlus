package com.github.james9909.warplus.command.admin

import com.github.james9909.warplus.WARPLUS_BASE_COMMAND
import com.github.james9909.warplus.WarPlus
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import java.lang.NumberFormatException
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AdminStatsCommand : AdminCommand() {
    override val usageString = "/$WARPLUS_BASE_COMMAND adminstats <clear|view <player>|addheal <player> <amount>>"
    override val description = "Manage warzone stats"

    override fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean {
        if (args.isEmpty()) return false
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
                val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId)
                if (playerInfo == null) {
                    if (sender is Player) {
                        plugin.playerManager.sendMessage(sender, "${player.name} is not participating in a warzone.")
                    }
                    return true
                }
                val warzone = playerInfo.team.warzone
                plugin.server.scheduler.runTaskAsynchronously(plugin) addHealTask@{ _ -> // Needed to remove ambiguity
                    warzone.statTracker?.addHeal(player.uniqueId, amount)
                    if (sender is Player) {
                        plugin.playerManager.sendMessage(sender, "Heal added for ${player.name}.")
                    }
                }
            }
            "view" -> {
                if (args.size < 2) return false
                val player = plugin.server.getPlayer(args[1])
                if (player == null) {
                    plugin.playerManager.sendMessage(sender, "No such player ${args[1]}")
                    return true
                }
                val dbm = plugin.databaseManager
                if (dbm == null) {
                    plugin.playerManager.sendMessage(sender, "Stats are disabled.")
                    return true
                }
                plugin.server.scheduler.runTaskAsynchronously(plugin) { _ ->
                    val stats = dbm.getPlayerStat(player.uniqueId)
                    if (stats == null) {
                        plugin.playerManager.sendMessage(sender, "${player.name} has not completed a warzone yet.")
                    } else {
                        stats.sendToPlayer(plugin, player)
                    }
                }
            }
        }
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> {
                listOf("clear", "addheal", "view").filter {
                    it.startsWith(args[0])
                }
            }
            else -> emptyList()
        }
    }
}
