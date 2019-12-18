package com.github.james9909.warplus

sealed class WarError(message: String)

data class IllegalWarzoneError(val message: String) : WarError(message)

data class WarSqlError(val message: String) : WarError(message)