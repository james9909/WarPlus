package com.github.james9909.warplus.managers

import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.WarSqlError
import com.github.james9909.warplus.extensions.toBytes
import com.github.james9909.warplus.sql.ConnectionFactory
import com.github.james9909.warplus.sql.models.AbstractModel
import com.github.james9909.warplus.sql.models.PlayerStatModel
import com.github.james9909.warplus.sql.models.WarzoneModel
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.UUID

class DatabaseManager(
    private val plugin: WarPlus,
    private val connectionFactory: ConnectionFactory
) {
    private fun runSql(func: (conn: Connection) -> Unit): Result<Boolean, WarSqlError> {
        return when (val connResult = connectionFactory.getConnection()) {
            is Err -> {
                plugin.logger.info(connResult.error.toString())
                connResult
            }
            is Ok -> {
                val conn = connResult.unwrap()
                try {
                    conn.autoCommit = false
                    func(conn)
                    conn.commit()
                } catch (e: SQLException) {
                    conn.rollback()
                    plugin.logger.info("Failed to execute SQL: $e")
                    e.printStackTrace()
                    return Err(WarSqlError("Failed to execute SQL: $e"))
                }
                conn.close()
                Ok(true)
            }
        }
    }

    fun createTables(): Result<Boolean, WarSqlError> {
        return runSql { conn ->
            conn.createStatement().use { statement ->
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `players` (`uuid` BINARY(16) NOT NULL, PRIMARY KEY (`uuid`))")
                statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `player_stats` (
                    `id` BINARY(16) PRIMARY KEY,
                    `kills` INTEGER NOT NULL,
                    `deaths` INTEGER NOT NULL,
                    `heals` INTEGER NOT NULL,
                    `wins` INTEGER NOT NULL,
                    `losses` INTEGER NOT NULL,
                    `flag_captures` INTEGER NOT NULL
                )""".trimIndent()
                )
                statement.executeUpdate(
                    """
                CREATE TABLE IF NOT EXISTS `warzones` (
                    `id` INTEGER PRIMARY KEY,
                    `start_time` DATETIME NOT NULL,
                    `end_time` DATETIME,
                    `winner` TEXT
                )""".trimIndent()
                )
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS `pk_warzones_id` on `warzones`(`id`)")
                statement.executeUpdate(
                    """
                CREATE TABLE IF NOT EXISTS `warzone_join_logs` (
                     `warzone_id` INT NOT NULL,
                     `player_id` BINARY(16) NOT NULL,
                     `team` VARCHAR(16) NOT NULL,
                     `join_time` DATETIME NOT NULL,
                     `leave_time` DATETIME,
                     FOREIGN KEY (`player_id`) REFERENCES `players`(`uuid`),
                     FOREIGN KEY (`warzone_id`) REFERENCES `warzones`(`id`)
                )""".trimIndent()
                )
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS `warzone_join_logs_player_fk` on `warzone_join_logs`(`player_id`)")
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS `warzone_join_logs_warzone_fk` on `warzone_join_logs`(`warzone_id`)")
                statement.executeUpdate(
                    """
                CREATE TABLE IF NOT EXISTS kills (
                    `date` DATETIME NOT NULL,
                    `warzone_id` INT NOT NULL,
                    `attacker_id` BINARY(16) NOT NULL,
                    `defender_id` BINARY(16) NOT NULL,
                    `attacker_class` VARCHAR(16) NOT NULL,
                    `defender_class` VARCHAR(16) NOT NULL,
                    FOREIGN KEY (`attacker_id`) REFERENCES `players`(`uuid`),
                    FOREIGN KEY (`defender_id`) REFERENCES `players`(`uuid`)
                )""".trimIndent()
                )
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS `kills_attacker_fk` on `kills`(`attacker_id`)")
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS `kills_defender_fk` on `kills`(`defender_id`)")
            }
        }
    }

    fun dropTables(): Result<Boolean, WarSqlError> {
        return runSql { conn ->
            conn.createStatement().use { statement ->
                statement.execute("DROP TABLE `warzones`")
                statement.execute("DROP TABLE `warzone_join_logs`")
                statement.execute("DROP TABLE `kills`")
                statement.execute("DROP TABLE `player_stats`")
                statement.execute("DROP TABLE `players`")
            }
        }
    }

    fun init() {
        connectionFactory.init()
    }

    fun close() {
        connectionFactory.close()
    }

    fun addPlayer(uuid: UUID) {
        runSql { conn ->
            conn.prepareStatement("INSERT OR IGNORE INTO `players` (`uuid`) VALUES (?)").use { statement ->
                statement.setBytes(1, uuid.toBytes())
                statement.executeUpdate()
            }
        }
    }

    fun addWarzone(): Int {
        var warzoneId = -1
        runSql { conn ->
            conn.prepareStatement(
                "INSERT INTO `warzones` (`start_time`) VALUES (CURRENT_TIMESTAMP)",
                PreparedStatement.RETURN_GENERATED_KEYS
            ).use { statement ->
                statement.executeUpdate()
                val rs = statement.generatedKeys
                if (rs.next()) {
                    warzoneId = rs.getInt(1)
                }
            }
        }
        return warzoneId
    }

    fun endWarzone(id: Int, winners: List<TeamKind>) {
        runSql { conn ->
            conn.prepareStatement("UPDATE `warzones` SET `end_time` = CURRENT_TIMESTAMP, `winner` = ? WHERE `id` = ?").use { statement ->
                statement.setString(1, winners.joinToString(","))
                statement.setInt(2, id)
                statement.executeUpdate()
            }
        }
    }

    fun getWarzone(id: Int): WarzoneModel? {
        var data: WarzoneModel? = null
        runSql { conn ->
            conn.prepareStatement("SELECT `id`, `start_time`, `end_time`, `winner` FROM `warzones` WHERE `id` = ?").use { statement ->
                statement.setInt(1, id)
                val rs = statement.executeQuery()
                if (rs.next()) {
                    val winners = rs.getString(4)
                    val parsedWinners = winners?.split(",")?.map {
                        TeamKind.valueOf(it.toUpperCase())
                    }
                    data = WarzoneModel(
                        id,
                        rs.getTimestamp(2),
                        rs.getTimestamp(3),
                        parsedWinners
                    )
                }
            }
        }
        return data
    }

    fun getPlayerStat(playerId: UUID): PlayerStatModel? {
        var data: PlayerStatModel? = null
        runSql { conn ->
            conn.prepareStatement("SELECT `kills`, `deaths`, `heals`, `wins`, `losses`, `flag_captures` FROM `player_stats` WHERE `id` = ?").use { statement ->
                statement.setBytes(1, playerId.toBytes())
                val rs = statement.executeQuery()
                if (rs.next()) {
                    data = PlayerStatModel(
                        playerId,
                        rs.getInt(1),
                        rs.getInt(2),
                        rs.getInt(3),
                        rs.getInt(4),
                        rs.getInt(5),
                        rs.getInt(6)
                    )
                }
            }
        }
        return data
    }

    fun writeModels(models: Collection<AbstractModel>) {
        runSql { conn ->
            models.forEach { model ->
                model.write(conn)
            }
        }
    }
}
