package com.github.james9909.warplus.sql.models

import com.github.james9909.warplus.TeamKind
import java.sql.Connection
import java.sql.Timestamp

data class WarzoneModel(
    val id: Int,
    val startTime: Timestamp,
    val endTime: Timestamp?,
    val winners: List<TeamKind>?
) : AbstractModel() {
    override fun write(conn: Connection) {
        conn.prepareStatement(
            """
            INSERT INTO `warzones` (`id`, `start_time`, `end_time`, `winners`) VALUES (?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.setInt(1, id)
            statement.setTimestamp(2, startTime)
            statement.setTimestamp(3, endTime)
            statement.setString(4, winners?.joinToString(","))
            statement.executeUpdate()
        }
    }
}
