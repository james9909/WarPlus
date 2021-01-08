package com.github.james9909.warplus.sql.models

import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.extensions.toBytes
import java.sql.Connection
import java.sql.Timestamp
import java.util.UUID

class WarzoneJoinLog(
    val warzoneId: Int,
    val playerId: UUID,
    var team: TeamKind,
    val joinTime: Timestamp,
    var leaveTime: Timestamp?
) : AbstractModel() {
    override fun write(conn: Connection) {
        conn.prepareStatement(
            """
            INSERT INTO `warzone_join_logs` (`warzone_id`, `player_id`, `team`, `join_time`, `leave_time`) VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.setInt(1, warzoneId)
            statement.setBytes(2, playerId.toBytes())
            statement.setString(3, team.toString())
            statement.setTimestamp(4, joinTime)
            statement.setTimestamp(5, leaveTime)
            statement.executeUpdate()
        }
    }
}
