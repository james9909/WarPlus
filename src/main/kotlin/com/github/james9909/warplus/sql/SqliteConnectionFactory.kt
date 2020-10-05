package com.github.james9909.warplus.sql

import com.github.james9909.warplus.WarSqlError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class SqliteConnectionFactory(private val fileName: String) : ConnectionFactory {
    override fun getConnection(): Result<Connection, WarSqlError> {
        return try {
            Class.forName("org.sqlite.JDBC")
            Ok(DriverManager.getConnection("jdbc:sqlite:$fileName"))
        } catch (e: SQLException) {
            Err(WarSqlError(e.message ?: "Failed to create SQL connection"))
        }
    }

    override fun init() { /* no-op */ }
    override fun close() { /* no-op */ }
}
