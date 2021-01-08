package com.github.james9909.warplus.sql.models

import com.github.james9909.warplus.extensions.toBytes
import java.sql.Connection
import java.sql.Timestamp
import java.util.UUID

data class KillModel(
    val warzoneId: Int,
    val date: Timestamp,
    val attackerId: UUID,
    val defenderId: UUID,
    val attackerClass: String,
    val defenderClass: String
) : AbstractModel() {
    override fun write(conn: Connection) {
        conn.prepareStatement(
            """
            INSERT INTO `kills` (`date`, `warzone_id`, `attacker_id`, `defender_id`, `attacker_class`, `defender_class`)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.setTimestamp(1, date)
            statement.setInt(2, warzoneId)
            statement.setBytes(3, attackerId.toBytes())
            statement.setBytes(4, defenderId.toBytes())
            statement.setString(5, attackerClass)
            statement.setString(6, defenderClass)
            statement.executeUpdate()
        }
    }
}
