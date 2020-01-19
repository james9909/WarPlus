package com.github.james9909.warplus.command.regular

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.command.AbstractCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ClassCommand(plugin: WarPlus, sender: CommandSender, args: List<String>) : AbstractCommand(plugin, sender, args) {
    override fun handle(): Boolean {
        if (args.isEmpty()) {
            return false
        }
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that")
            return true
        }
        val playerInfo = plugin.playerManager.getPlayerInfo(sender)
        if (playerInfo == null) {
            plugin.playerManager.sendMessage(sender, "You are not in a warzone")
            return true
        }
        if (!playerInfo.inSpawn) {
            plugin.playerManager.sendMessage(sender, "You are not in spawn")
            return true
        }
        val className = args[0]
        val warClass = plugin.classManager.getClass(className)
        if (warClass == null) {
            plugin.playerManager.sendMessage(sender, "Class $className does not exist")
            return true
        }
        sender.inventory.clear()
        playerInfo.warClass = warClass
        warClass.giveToPlayer(sender)
        plugin.playerManager.sendMessage(sender, "Equipped $className class")
        return true
    }
}