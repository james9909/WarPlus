package com.github.james9909.warplus.sql

import com.github.james9909.warplus.WarSqlError
import com.github.michaelbull.result.Result
import java.sql.Connection

interface ConnectionFactory {
    fun getConnection(): Result<Connection, WarSqlError>
    fun init()
    fun close()
}
