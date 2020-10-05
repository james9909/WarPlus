package com.github.james9909.warplus.command.player

import com.github.james9909.warplus.WARPLUS_BASE_COMMAND
import com.github.james9909.warplus.WarPlus
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ClassCommand : PlayerCommand() {
    override val usageString = "/$WARPLUS_BASE_COMMAND class <name>"
    override val description = "Select and equip a class"

    override fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean {
        if (args.isEmpty()) {
            return false
        }
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that")
            return true
        }
        val playerInfo = plugin.playerManager.getPlayerInfo(sender)
        if (playerInfo == null) {
            plugin.playerManager.sendMessage(sender, "You are not playing in a warzone")
            return true
        }
        if (!playerInfo.inSpawn) {
            plugin.playerManager.sendMessage(sender, "You are not in spawn")
            return true
        }
        val className = args[0]
        val availableClasses = playerInfo.team.resolveClasses().map { it.toLowerCase() }
        if (className.toLowerCase() !in availableClasses) {
            plugin.playerManager.sendMessage(sender, "Class $className is not enabled in this warzone")
            return true
        }

        val warClass = plugin.classManager.getClass(className)
        if (warClass == null) {
            plugin.playerManager.sendMessage(sender, "Class $className does not exist")
            return true
        }
        sender.inventory.clear()
        playerInfo.team.warzone.equipClass(sender, warClass, true)

        plugin.playerManager.sendMessage(sender, "Equipped $className class")
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        return emptyList()
    }
}
