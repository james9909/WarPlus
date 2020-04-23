package com.github.james9909.warplus.config

class WarzoneConfigType {
    companion object {
        val ENABLED = booleanKey("enabled", true)
        val CLASS_CMD = stringKey("class-cmd", "")
        val DEFAULT_CLASS = stringKey("default-class", "")
        val MIN_TEAMS = integerKey("min-teams", 2)
        val DEATH_MESSAGES = booleanKey("death-messages", true)
        val MAX_HEALTH = doubleKey("max-health", 20.0)
    }
}
