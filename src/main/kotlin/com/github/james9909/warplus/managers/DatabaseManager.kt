package com.github.james9909.warplus.managers

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.WarSqlError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import java.sql.Connection
import java.sql.DriverManager

class DatabaseManager(private val plugin: WarPlus, private val database: String) {

    private fun getConnection(): Result<Connection, WarSqlError> {
        return try {
            Class.forName("org.sqlite.JDBC")
            Ok(DriverManager.getConnection(database))
        } catch (e: Exception) {
            Err(WarSqlError("Failed to connect to the database: $e"))
        }
    }

    private fun runSql(func: (conn: Connection) -> Unit): Result<Boolean, WarSqlError> {
        return when (val connResult = getConnection()) {
            is Err -> {
                plugin.logger.info(connResult.error.toString())
                connResult
            }
            is Ok -> {
                val conn = connResult.unwrap()
                try {
                    func(conn)
                } catch (e: Exception) {
                    conn.close()
                    plugin.logger.info("Failed to execute SQL: $e")
                    return Err(WarSqlError("Failed to execute SQL: $e"))
                }
                conn.close()
                Ok(true)
            }
        }
    }

    fun createTables(): Result<Boolean, WarSqlError> {
        return runSql { conn ->
            val statement = conn.createStatement()
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS players (uuid BINARY(16) NOT NULL, PRIMARY KEY (uuid))")
            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS heals (
                    date DATETIME NOT NULL,
                    healer_id BINARY(16) NOT NULL,
                    target_id BINARY(16) NOT NULL,
                    amount DOUBLE NOT NULL,
                    class VARCHAR(16) NOT NULL,
                    FOREIGN KEY (healer_id) REFERENCES players(uuid),
                    FOREIGN KEY (target_id) REFERENCES players(uuid)
                )
            """.trimIndent()
            )
            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS kills (
                    date DATETIME NOT NULL,
                    attacker_id BINARY(16) NOT NULL,
                    defender_id BINARY(16) NOT NULL,
                    attacker_class VARCHAR(16) NOT NULL,
                    defender_class VARCHAR(16) NOT NULL,
                    FOREIGN KEY (attacker_id) REFERENCES players(uuid),
                    FOREIGN KEY (defender_id) REFERENCES players(uuid)
                )
            """.trimIndent()
            )
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_attacker on kills(attacker_id)")
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_defender on kills(defender_id)")
            statement.close()
        }
    }

    fun dropTables(): Result<Boolean, WarSqlError> {
        return runSql { conn ->
            val statement = conn.createStatement()
            statement.execute("DROP TABLE kills")
            statement.execute("DROP TABLE heals")
            statement.execute("DROP TABLE players")
            statement.close()
        }
    }
}