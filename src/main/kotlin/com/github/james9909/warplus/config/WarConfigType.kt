package com.github.james9909.warplus.config

enum class DatabaseDialect {
    MYSQL,
    SQLITE
}

class WarConfigType private constructor() {
    companion object {
        val DATABASE_ENABLED = booleanKey("global.database.enabled", false)
        val DATABASE_SERVER = stringKey("global.database.server", "localhost")
        val DATABASE_PORT = integerKey("global.database.port", 3306)
        val DATABASE_NAME = stringKey("global.database.database-name", "WarPlus")
        val DATABASE_USERNAME = stringKey("global.database.username", "root")
        val DATABASE_PASSWORD = stringKey("global.database.password", "password")
        val DATABASE_FILENAME = stringKey("global.database.filename", "warplus.db")
        val DATABASE_DIALECT = enumKey("global.database.dialect", DatabaseDialect.SQLITE)
        val RESTORE_PLAYER_LOCATION = booleanKey("global.restore-player-location", true)
        val ALLOWED_WARZONE_COMMANDS = stringListKey("global.allowed-commands", listOf())
    }
}
