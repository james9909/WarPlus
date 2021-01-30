package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.WARPLUS_BASE_COMMAND
import com.github.james9909.warplus.WarPlus
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class DeleteBombCommand : ZonemakerCommand() {
    override val usageString = "/$WARPLUS_BASE_COMMAND deletebomb [name]"
    override val description = "Delete a bomb by its name. " +
        "If no name is provided, delete the bomb at your current location."

    override fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that")
            return true
        }
        val warzone = plugin.warzoneManager.getWarzoneByLocation(sender.location)
        if (warzone == null) {
            plugin.playerManager.sendMessage(sender, "You're not in a warzone")
            return true
        }
        val bomb = if (args.isEmpty()) {
            warzone.getBombAtLocation(sender.location)
        } else {
            warzone.getBombByName(args[0])
        }
        if (bomb == null) {
            plugin.playerManager.sendMessage(sender, "Bomb not found")
            return true
        }
        bomb.restoreVolume()
        warzone.removeBomb(bomb)
        warzone.saveConfig()
        plugin.playerManager.sendMessage(sender, "Bomb ${bomb.name} has been deleted")
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        return emptyList()
    }
}
