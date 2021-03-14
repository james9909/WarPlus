package com.github.james9909.warplus.sql.models

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.extensions.color
import com.github.james9909.warplus.extensions.toBytes
import java.sql.Connection
import java.util.UUID
import org.bukkit.entity.Player

class PlayerStatModel(
    var id: UUID,
    var kills: Int,
    var deaths: Int,
    var heals: Int,
    var wins: Int,
    var losses: Int,
    var flagCaptures: Int,
    var bombs: Int,
    var mvps: Int
) : AbstractModel() {
    override fun write(conn: Connection) {
        // We only want to upsert here

        conn.prepareStatement(
            """
            INSERT OR IGNORE INTO `player_stats` (`id`, `kills`, `deaths`, `heals`, `wins`, `losses`, `flag_captures`, `bombs`, `mvps`)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { insert ->
            insert.setBytes(1, id.toBytes())
            insert.setInt(2, kills)
            insert.setInt(3, deaths)
            insert.setInt(4, heals)
            insert.setInt(5, wins)
            insert.setInt(6, losses)
            insert.setInt(7, flagCaptures)
            insert.setInt(8, bombs)
            insert.setInt(9, mvps)
            if (insert.executeUpdate() == 0) {
                // Insert ignored, update the row instead
                conn.prepareStatement(
                    """
                    UPDATE `player_stats` SET
                        `kills` = `kills` + ?,
                        `deaths` = `deaths` + ?,
                        `heals` = `heals` + ?,
                        `wins` = `wins` + ?,
                        `losses` = `losses` + ?,
                        `flag_captures` = `flag_captures` + ?,
                        `bombs` = `bombs` + ?,
                        `mvps` = `mvps` + ?
                        WHERE `id` = ?
                    """.trimIndent()
                ).use { update ->
                    update.setInt(1, kills)
                    update.setInt(2, deaths)
                    update.setInt(3, heals)
                    update.setInt(4, wins)
                    update.setInt(5, losses)
                    update.setInt(6, flagCaptures)
                    update.setInt(7, bombs)
                    update.setInt(8, mvps)
                    update.setBytes(9, id.toBytes())
                    update.executeUpdate()
                }
            }
        }
    }

    fun sendToPlayer(plugin: WarPlus, player: Player) {
        plugin.playerManager.sendMessage(player, "&8&m------------------".color(), withPrefix = false)
        plugin.playerManager.sendMessage(player, "&9&lStats".color(), withPrefix = false)
        plugin.playerManager.sendMessage(player, "   &bWins: &a$wins".color(), withPrefix = false)
        plugin.playerManager.sendMessage(player, "   &bLosses: &a$losses".color(), withPrefix = false)
        plugin.playerManager.sendMessage(player, "   &bKills: &a$kills".color(), withPrefix = false)
        plugin.playerManager.sendMessage(player, "   &bDeaths: &a$deaths".color(), withPrefix = false)
        plugin.playerManager.sendMessage(player, "   &bHeals: &a$heals".color(), withPrefix = false)
        plugin.playerManager.sendMessage(player, "   &bFlag captures: &a$flagCaptures".color(), withPrefix = false)
        plugin.playerManager.sendMessage(player, "   &bBombs: &a$bombs".color(), withPrefix = false)
        plugin.playerManager.sendMessage(player, "   &bMVPs: &a$mvps".color(), withPrefix = false)
        plugin.playerManager.sendMessage(player, "&8&m------------------".color(), withPrefix = false)
    }

    companion object {
        fun default(id: UUID): PlayerStatModel = PlayerStatModel(
            id,
            kills = 0,
            deaths = 0,
            heals = 0,
            wins = 0,
            losses = 0,
            flagCaptures = 0,
            bombs = 0,
            mvps = 0
        )
    }
}
