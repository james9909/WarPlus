package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.WARPLUS_BASE_COMMAND
import com.github.james9909.warplus.WarPlus
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class DeleteMonumentCommand : ZonemakerCommand() {
    override val usageString = "/$WARPLUS_BASE_COMMAND deletemonument [name]"
    override val description = "Delete a monument by its name. " +
        "If no name is provided, delete the monument at your current location."

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
        val monument = if (args.isEmpty()) {
            warzone.getMonumentAtLocation(sender.location)
        } else {
            warzone.getMonumentByName(args[0])
        }
        if (monument == null) {
            plugin.playerManager.sendMessage(sender, "Monument not found")
            return true
        }
        monument.restoreVolume()
        warzone.removeMonument(monument)
        warzone.saveConfig()
        plugin.playerManager.sendMessage(sender, "Monument ${monument.name} has been deleted")
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        return emptyList()
    }
}
