package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.WARPLUS_BASE_COMMAND
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.command.AbstractCommand
import org.bukkit.Material
import org.bukkit.command.CommandSender

class DeletePortalCommand : AbstractCommand() {
    override val USAGE_STRING = "/$WARPLUS_BASE_COMMAND deleteportal <warzone name> <portal name>"
    override val DESCRIPTION = "Delete a warzone portal by its name"

    override fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean {
        if (args.size < 2) {
            return false
        }

        val warzone = plugin.warzoneManager.getWarzone(args[0])
        if (warzone == null) {
            plugin.playerManager.sendMessage(sender, "That warzone doesn't exist")
            return true
        }
        val portal = warzone.getPortalByName(args[1])
        if (portal == null) {
            plugin.playerManager.sendMessage(sender, "No portal with that name exists")
            return true
        }
        portal.restoreVolume()
        portal.signBlock.type = Material.AIR
        warzone.removePortal(portal)
        warzone.saveConfig()
        portal.deleteVolume()
        plugin.playerManager.sendMessage(sender, "Portal ${portal.name} deleted!")
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        val warzoneNames = plugin.warzoneManager.getWarzoneNames()
        return when (args.size) {
            1 -> warzoneNames.filter {
                it.startsWith(args[0].toLowerCase())
            }
            2 -> plugin.warzoneManager.getWarzones().map { warzone ->
                    warzone.getPortals().map { portal -> portal.name }
            }.flatten()
            .filter {
                it.startsWith(args[1].toLowerCase())
            }
            else -> emptyList()
        }
    }
}