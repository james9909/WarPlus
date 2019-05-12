package com.github.james9909.warplus.managers

import com.github.james9909.warplus.Team
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.util.Message
import com.github.james9909.warplus.util.PlayerState
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

data class PlayerInfo(
    var team: Team?,
    var state: PlayerState
)

class PlayerManager(plugin: WarPlus) {
    private val players = ConcurrentHashMap<Player, PlayerInfo>()
    private val chatPrefix: String

    init {
        var tempPrefix = plugin.config.getString("global.prefix") ?: ""
        if (tempPrefix.contains("&")) {
            tempPrefix = ChatColor.translateAlternateColorCodes('&', tempPrefix)
        }
        chatPrefix = tempPrefix
    }

    fun sendMessage(sender: CommandSender, message: String) {
        if (sender is Player) {
            sender.sendMessage("$chatPrefix${ChatColor.RESET}$message${ChatColor.RESET}")
        } else {
            sender.sendMessage(message)
        }
    }

    fun sendMessage(sender: CommandSender, message: Message) {
        sendMessage(sender, message.msg)
    }

    fun getPlayerInfo(player: Player): PlayerInfo? {
        return players[player]
    }

    fun savePlayerState(player: Player, team: Team?) {
        if (!players.containsKey(player)) {
            players[player] = PlayerInfo(team, PlayerState(player))
        } else {
            players[player]?.team = team
            players[player]?.state?.update()
        }
    }

    fun restorePlayerState(player: Player) {
        players[player]?.state?.restore()
    }
}