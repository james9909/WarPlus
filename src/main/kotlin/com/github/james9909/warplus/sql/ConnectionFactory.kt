package com.github.james9909.warplus.sql

import com.github.james9909.warplus.WarSqlError
import com.github.michaelbull.result.Result
import java.sql.Connection

abstract class ConnectionFactory {
    abstract fun getConnection(): Result<Connection, WarSqlError>
    abstract fun init()
    abstract fun close()
}