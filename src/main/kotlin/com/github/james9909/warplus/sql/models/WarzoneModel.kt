package com.github.james9909.warplus.sql.models

import com.github.james9909.warplus.TeamKind
import java.sql.Connection
import java.sql.Timestamp

data class WarzoneModel(
    val id: Int,
    val name: String,
    val startTime: Timestamp,
    val endTime: Timestamp?,
    val winners: List<TeamKind>?
) : AbstractModel() {
    override fun write(conn: Connection) {
        conn.prepareStatement(
            """
            INSERT INTO `warzones` (`id`, `name`, `start_time`, `end_time`, `winners`) VALUES (?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.setInt(1, id)
            statement.setString(2, name)
            statement.setTimestamp(3, startTime)
            statement.setTimestamp(4, endTime)
            statement.setString(5, winners?.joinToString(","))
            statement.executeUpdate()
        }
    }
}
