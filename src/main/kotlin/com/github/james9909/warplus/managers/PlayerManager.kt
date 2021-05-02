package com.github.james9909.warplus.managers

import com.github.james9909.warplus.WarTeam
import com.github.james9909.warplus.WarClass
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.util.LastDamager
import com.github.james9909.warplus.util.Message
import com.github.james9909.warplus.util.PlayerState
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionAttachment
import java.util.UUID

sealed class WarParticipant {
    data class Player(
        val team: WarTeam,
        val state: PlayerState,
        var inSpawn: Boolean,
        var warClass: WarClass?,
        val lastDamager: LastDamager,
        var respawnTime: Long = 0
    ) : WarParticipant()

    data class Spectator(val state: PlayerState, val warzone: Warzone) : WarParticipant()
}

class PlayerManager(val plugin: WarPlus) {
    private val players = HashMap<UUID, WarParticipant>()
    private val permissions = HashMap<UUID, PermissionAttachment>()
    private val chatPrefix: String

    init {
        var tempPrefix = plugin.config.getString("global.prefix") ?: ""
        if (tempPrefix.contains("&")) {
            tempPrefix = ChatColor.translateAlternateColorCodes('&', tempPrefix)
        }
        chatPrefix = tempPrefix
    }

    fun sendMessage(sender: CommandSender, message: String, withPrefix: Boolean = true) {
        if (sender is Player) {
            if (withPrefix) {
                sender.sendMessage("$chatPrefix${ChatColor.RESET}$message${ChatColor.RESET}")
            } else {
                sender.sendMessage("${ChatColor.RESET}$message${ChatColor.RESET}")
            }
        } else {
            sender.sendMessage(message)
        }
    }

    fun sendMessage(sender: CommandSender, message: Message) {
        sendMessage(sender, message.msg)
    }

    fun getParticipantInfo(uuid: UUID): WarParticipant? = players[uuid]

    fun getPlayerInfo(uuid: UUID): WarParticipant.Player? = when (val p = players[uuid]) {
        is WarParticipant.Player -> p
        else -> null
    }

    fun setPlayerInfo(uuid: UUID, info: WarParticipant.Player) {
        players[uuid] = info
    }

    fun getSpectatorInfo(uuid: UUID): WarParticipant.Spectator? = when (val p = players[uuid]) {
        is WarParticipant.Spectator -> p
        else -> null
    }

    fun savePlayerState(player: Player, team: WarTeam, saveLocation: Boolean): WarParticipant.Player {
        val playerInfo = WarParticipant.Player(
            team,
            PlayerState.fromPlayer(player, saveLocation),
            inSpawn = true,
            warClass = null,
            lastDamager = LastDamager(null)
        )
        players[player.uniqueId] = playerInfo
        plugin.inventoryManager.saveInventory(player)
        return playerInfo
    }

    fun saveSpectatorState(player: Player, warzone: Warzone, saveLocation: Boolean): WarParticipant.Spectator {
        val playerInfo = WarParticipant.Spectator(PlayerState.fromPlayer(player, saveLocation), warzone)
        players[player.uniqueId] = playerInfo
        return playerInfo
    }

    fun removePlayer(uuid: UUID) = players.remove(uuid)

    fun getPermissions(player: Player): PermissionAttachment {
        if (permissions.containsKey(player.uniqueId)) {
            return permissions[player.uniqueId]!!
        }
        val p = player.addAttachment(plugin)
        permissions[player.uniqueId] = p
        return p
    }
}
