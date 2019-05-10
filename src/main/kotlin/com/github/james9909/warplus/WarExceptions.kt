package com.github.james9909.warplus

open class WarException(message: String) : Exception(message)

open class IllegalWarzoneException(message: String) : com.github.james9909.warplus.WarException(message)
