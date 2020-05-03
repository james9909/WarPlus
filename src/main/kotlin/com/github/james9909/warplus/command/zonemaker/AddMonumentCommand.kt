package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.command.AbstractCommand
import com.github.james9909.warplus.extensions.blockLocation
import com.github.james9909.warplus.structures.MonumentStructure
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AddMonumentCommand(plugin: WarPlus, sender: CommandSender, args: List<String>) :
    AbstractCommand(plugin, sender, args) {

    override fun handle(): Boolean {
        if (args.isEmpty()) {
            return false
        }
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that")
            return true
        }
        val warzone = plugin.warzoneManager.getWarzoneByLocation(sender.location)
        if (warzone == null) {
            plugin.playerManager.sendMessage(sender, "You're not in a warzone")
            return true
        }
        val monumentStructure =
            MonumentStructure(plugin, sender.location.subtract(0.0, 1.0, 0.0).blockLocation(), args[0])
        monumentStructure.saveVolume()
        monumentStructure.build()

        warzone.addMonument(monumentStructure)
        warzone.saveConfig()
        plugin.playerManager.sendMessage(sender, "Monument ${args[0]} created!")
        return true
    }
}