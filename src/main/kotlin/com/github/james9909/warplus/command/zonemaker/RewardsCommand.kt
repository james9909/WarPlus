package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.WARPLUS_BASE_COMMAND
import com.github.james9909.warplus.WarPlus
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.lang.NumberFormatException

class RewardsCommand : ZonemakerCommand() {
    override val usageString = "/$WARPLUS_BASE_COMMAND rewards <warzone> <win|loss> <add|remove [index]|list>"
    override val description = "Manage warzone rewards"

    override fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean {
        if (args.size < 3) {
            return false
        }
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that.")
            return true
        }
        val warzone = plugin.warzoneManager.getWarzone(args[0])
        if (warzone == null) {
            plugin.playerManager.sendMessage(sender, "That warzone doesn't exist.")
            return true
        }
        val reward = when (args[1].toLowerCase()) {
            "win" -> warzone.reward.winReward
            "loss" -> warzone.reward.lossReward
            else -> {
                plugin.playerManager.sendMessage(sender, "Reward type must be 'win' or 'loss'.")
                return true
            }
        }
        when (args[2].toLowerCase()) {
            "add" -> {
                val item = sender.inventory.itemInMainHand
                if (item.type == Material.AIR) {
                    plugin.playerManager.sendMessage(sender, "You must hold the item you wish to add.")
                    return true
                }
                reward.add(Pair(sender.inventory.itemInMainHand, null))
                warzone.saveConfig()
                plugin.playerManager.sendMessage(sender, "Reward added.")
            }
            "remove" -> {
                val idx = try {
                    if (args.size == 3) {
                        0
                    } else {
                        args[3].toInt() - 1
                    }
                } catch (e: NumberFormatException) {
                    plugin.playerManager.sendMessage(sender, "The reward to remove must be a number.")
                    return true
                }
                if (idx >= reward.size) {
                    plugin.playerManager.sendMessage(sender, "Invalid reward '$idx'.")
                    return true
                }
                reward.removeAt(idx)
                warzone.saveConfig()
                plugin.playerManager.sendMessage(sender, "Reward removed.")
            }
            "list" -> {
                plugin.playerManager.sendMessage(sender, "Rewards for ${warzone.name}:")
                reward.forEachIndexed { i, item ->
                    val displayName = item.first.run {
                        if (hasItemMeta()) {
                            if (itemMeta!!.hasDisplayName()) {
                                itemMeta!!.displayName
                            } else {
                                type.toString()
                            }
                        } else {
                            type.toString()
                        }
                    }
                    plugin.playerManager.sendMessage(sender, "${i + 1}: ${item.first.amount} $displayName")
                }
            }
        }
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> {
                plugin.warzoneManager.getWarzoneNames().filter {
                    it.startsWith(args[0])
                }
            }
            2 -> {
                listOf("win", "loss").filter {
                    it.startsWith(args[1])
                }
            }
            3 -> {
                listOf("add", "remove", "list").filter {
                    it.startsWith(args[2])
                }
            }
            else -> emptyList()
        }
    }
}
