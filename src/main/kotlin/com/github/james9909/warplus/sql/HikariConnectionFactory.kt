package com.github.james9909.warplus.sql

import com.github.james9909.warplus.WarSqlError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.SQLException

abstract class HikariConnectionFactory(
    private val server: String,
    private val port: Int,
    private val databaseName: String,
    private val username: String,
    private val password: String
) : ConnectionFactory {

    private lateinit var hikari: HikariDataSource

    override fun init() {
        val config = HikariConfig()
        config.poolName = "warplus-hikari"

        config.addDataSourceProperty("serverName", server)
        config.addDataSourceProperty("port", port)
        config.addDataSourceProperty("databaseName", databaseName)
        config.dataSourceClassName = getDriverClass()
        config.username = username
        config.password = password

        hikari = HikariDataSource(config)
    }

    override fun getConnection(): Result<Connection, WarSqlError> {
        return try {
            Ok(hikari.connection)
        } catch (e: SQLException) {
            Err(WarSqlError(e.message ?: "Failed to create SQL connection"))
        }
    }

    abstract fun getDriverClass(): String

    override fun close() {
        hikari.close()
    }
}
