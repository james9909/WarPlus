package com.github.james9909.warplus.sql.models

import com.github.james9909.warplus.extensions.toBytes
import java.sql.Connection
import java.util.UUID

class PlayerStatModel(var id: UUID, var kills: Int, var deaths: Int, var heals: Int, var wins: Int, var losses: Int, var flagCaptures: Int) : AbstractModel() {
    override fun write(conn: Connection) {
        // We only want to upsert here

        conn.prepareStatement(
            """
            INSERT OR IGNORE INTO `player_stats` (`id`, `kills`, `deaths`, `heals`, `wins`, `losses`, `flag_captures`)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { insert ->
            insert.setBytes(1, id.toBytes())
            insert.setInt(2, kills)
            insert.setInt(3, deaths)
            insert.setInt(4, heals)
            insert.setInt(5, wins)
            insert.setInt(6, losses)
            insert.setInt(7, flagCaptures)
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
                        `flag_captures` = `flag_captures` + ?
                        WHERE `id` = ?
                    """.trimIndent()
                ).use { update ->
                    update.setInt(1, kills)
                    update.setInt(2, deaths)
                    update.setInt(3, heals)
                    update.setInt(4, wins)
                    update.setInt(5, losses)
                    update.setInt(6, flagCaptures)
                    update.setBytes(7, id.toBytes())
                    update.executeUpdate()
                }
            }
        }
    }

    companion object {
        fun default(id: UUID): PlayerStatModel = PlayerStatModel(
            id,
            kills = 0,
            deaths = 0,
            heals = 0,
            wins = 0,
            losses = 0,
            flagCaptures = 0
        )
    }
}
