package com.github.james9909.warplus.command.player

import com.github.james9909.warplus.WARPLUS_BASE_COMMAND
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.WarzoneState
import com.github.james9909.warplus.util.Message
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class SpectateWarzoneCommand : PlayerCommand() {
    override val usageString = "/$WARPLUS_BASE_COMMAND spectate <warzone name>"
    override val description = "Spectate a warzone"

    override fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean {
        if (args.isEmpty()) {
            return false
        }
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that")
            return true
        }
        val playerInfo = plugin.playerManager.getParticipantInfo(sender)
        if (playerInfo != null) {
            plugin.playerManager.sendMessage(sender, "You already participating in a warzone")
            return true
        }

        val warzone = plugin.warzoneManager.getWarzone(args[0])
        if (warzone == null) {
            plugin.playerManager.sendMessage(sender, Message.NO_SUCH_WARZONE)
            return true
        }
        if (!warzone.isEnabled()) {
            plugin.playerManager.sendMessage(sender, Message.WARZONE_DISABLED)
            return true
        }
        if (warzone.state == WarzoneState.EDITING) {
            plugin.playerManager.sendMessage(sender, Message.WARZONE_EDITING)
            return true
        }

        warzone.addSpectator(sender)
        plugin.playerManager.sendMessage(sender, "You are now a spectator!")
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        return plugin.warzoneManager.getWarzoneNames().filter {
            it.startsWith(args[0].toLowerCase())
        }
    }
}
