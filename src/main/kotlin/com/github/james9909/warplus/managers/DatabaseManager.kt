package com.github.james9909.warplus.managers

import com.github.james9909.warplus.Err
import com.github.james9909.warplus.Ok
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.WarSqlException
import com.github.kittinunf.result.Result
import java.sql.Connection
import java.sql.DriverManager

class DatabaseManager(private val plugin: WarPlus, private val database: String) {

    private fun getConnection(): Result<Connection, WarSqlException> {
        return try {
            Class.forName("org.sqlite.JDBC")
            Ok(DriverManager.getConnection(database))
        } catch (e: Exception) {
            Err(WarSqlException("Failed to connect to the database: $e"))
        }
    }

    fun runSql(func: (conn: Connection) -> Unit): Result<Boolean, WarSqlException> {
        val connResult = getConnection()
        if (connResult is Err) {
            plugin.logger.info(connResult.error.toString())
            return connResult
        }

        val conn = connResult.get()
        try {
            func(conn)
        } catch (e: Exception) {
            conn.close()
            plugin.logger.info("Failed to execute SQL: $e")
            return Err(WarSqlException("Failed to execute SQL: $e"))
        }
        conn.close()
        return Ok(true)
    }

    fun createTables(): Result<Boolean, WarSqlException> {
        return runSql { conn ->
            val statement = conn.createStatement()
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS players (uuid BINARY(16) NOT NULL, PRIMARY KEY (uuid))")
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS heals (
                    date DATETIME NOT NULL,
                    healer_id BINARY(16) NOT NULL,
                    target_id BINARY(16) NOT NULL,
                    amount DOUBLE NOT NULL,
                    class VARCHAR(16) NOT NULL,
                    FOREIGN KEY (healer_id) REFERENCES players(uuid),
                    FOREIGN KEY (target_id) REFERENCES players(uuid)
                )
            """.trimIndent())
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS kills (
                    date DATETIME NOT NULL,
                    attacker_id BINARY(16) NOT NULL,
                    defender_id BINARY(16) NOT NULL,
                    attacker_class VARCHAR(16) NOT NULL,
                    defender_class VARCHAR(16) NOT NULL,
                    FOREIGN KEY (attacker_id) REFERENCES players(uuid),
                    FOREIGN KEY (defender_id) REFERENCES players(uuid)
                )
            """.trimIndent())
            statement.executeUpdate("CREATE INDEX idx_attacker on kills(attacker_id)")
            statement.executeUpdate("CREATE INDEX idx_defender on kills(defender_id)")
            statement.close()
        }
    }

    fun dropTables(): Result<Boolean, WarSqlException> {
        return runSql { conn ->
            val statement = conn.createStatement()
            statement.execute("DROP TABLE kills")
            statement.execute("DROP TABLE heals")
            statement.execute("DROP TABLE players")
            statement.close()
        }
    }
}