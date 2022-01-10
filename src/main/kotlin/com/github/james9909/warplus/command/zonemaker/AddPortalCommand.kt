package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.WARPLUS_BASE_COMMAND
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.extensions.blockLocation
import com.github.james9909.warplus.structures.WarzonePortalStructure
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

private fun getYawFromString(direction: String): Float? {
    return when (direction) {
        "n" -> 180F
        "e" -> 270F
        "s" -> 0F
        "w" -> 90F
        else -> null
    }
}

class AddPortalCommand : ZonemakerCommand() {
    override val usageString = "/$WARPLUS_BASE_COMMAND addportal <warzone name> <portal name> [<x> <y> <z> <n|e|s|w>]"
    override val description = "Create a warzone portal at the current location"

    override fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean {
        if (args.size < 2) return false
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that")
            return true
        }
        val warzone = plugin.warzoneManager.getWarzone(args[0])
        if (warzone == null) {
            plugin.playerManager.sendMessage(sender, "That warzone doesn't exist")
            return true
        }
        val existingPortal = warzone.getPortals().any { portal ->
            portal.name.equals(args[1], true)
        }
        if (existingPortal) {
            plugin.playerManager.sendMessage(sender, "A portal with that name already exists")
            return true
        }
        val origin = if (args.size == 6) {
            val yaw = getYawFromString(args[5])
            if (yaw == null) {
                plugin.playerManager.sendMessage(sender, "The direction must be either n, e, s, or w")
                return true
            }
            // Add 180 to the yaw so that the sign ends up facing the direction specified by the user
            Location(sender.world, args[2].toDouble(), args[3].toDouble(), args[4].toDouble(), yaw + 180, 0F)
        } else {
            sender.location.blockLocation()
        }
        val newPortal = WarzonePortalStructure(plugin, origin, args[1], warzone)
        val overlappingPortal = plugin.warzoneManager.getWarzones().any {
            it.getPortalByLocation(origin) != null
        }
        if (overlappingPortal) {
            plugin.playerManager.sendMessage(sender, "Portal cannot overlap with other portals")
            return true
        }
        newPortal.saveVolume()
        newPortal.build()
        warzone.addPortal(newPortal)
        warzone.saveConfig()
        plugin.playerManager.sendMessage(sender, "Portal ${newPortal.name} created!")
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> plugin.warzoneManager.getWarzoneNames().filter {
                it.startsWith(args[0].lowercase())
            }
            else -> emptyList()
        }
    }
}
