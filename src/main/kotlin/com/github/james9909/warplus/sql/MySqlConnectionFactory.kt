package com.github.james9909.warplus.sql

class MySqlConnectionFactory(
    server: String,
    port: Int,
    databaseName: String,
    username: String,
    password: String
) : HikariConnectionFactory(server, port, databaseName, username, password) {
    override fun getDriverClass(): String {
        return "com.mysql.jdbc.jdbc2.optional.MysqlDataSource"
    }
}
