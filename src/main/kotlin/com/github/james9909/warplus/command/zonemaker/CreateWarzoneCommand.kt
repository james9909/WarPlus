package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.command.AbstractCommand
import com.github.james9909.warplus.region.Region
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CreateWarzoneCommand(plugin: WarPlus, sender: CommandSender, args: List<String>) :
    AbstractCommand(plugin, sender, args) {

    override fun handle(): Boolean {
        if (args.isEmpty()) {
            return false
        }
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that")
            return true
        }

        var warzone = plugin.warzoneManager.getWarzone(args[0])
        if (warzone != null) {
            plugin.playerManager.sendMessage(sender, "A warzone with that name already exists")
            return true
        }
        warzone = Warzone(
            plugin,
            args[0],
            Region(sender.world)
        )
        plugin.warzoneManager.addWarzone(warzone)
        warzone.save()
        return true
    }
}
