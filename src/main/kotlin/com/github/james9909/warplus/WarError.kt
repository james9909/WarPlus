package com.github.james9909.warplus

sealed class WarError

data class IllegalWarzoneError(val message: String) : WarError() {
    override fun toString(): String {
        return message
    }
}

data class IllegalTeamKindError(val teamKind: String) : WarError() {
    override fun toString(): String {
        return "Invalid team kind: $teamKind"
    }
}

data class WarSqlError(val message: String) : WarError() {
    override fun toString(): String {
        return message
    }
}

data class FileError(val message: String) : WarError() {
    override fun toString(): String {
        return message
    }
}

data class InvalidSchematicError(val message: String) : WarError() {
    override fun toString(): String {
        return message
    }
}

data class WorldEditError(val message: String) : WarError() {
    override fun toString(): String {
        return message
    }
}

data class WarStructureError(val message: String) : WarError() {
    override fun toString(): String {
        return message
    }
}
