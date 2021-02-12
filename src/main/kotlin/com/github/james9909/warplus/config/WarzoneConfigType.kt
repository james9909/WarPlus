package com.github.james9909.warplus.config

class WarzoneConfigType private constructor() {
    companion object {
        val BLOCK_HEADS = booleanKey("block-heads", true)
        val CAN_BREAK_BLOCKS = booleanKey("can-break-blocks", true)
        val CLASS_CMD = stringKey("class-cmd", "")
        val DEATH_MESSAGES = booleanKey("death-messages", true)
        val ENABLED = booleanKey("enabled", true)
        val MAX_HEALTH = doubleKey("max-health", 20.0)
        val MIN_TEAMS = integerKey("min-teams", 2)
        val ITEM_DROPS = booleanKey("item-drops", false)
        val MONUMENT_HEAL = integerKey("monument-heal", 1)
        val MONUMENT_HEAL_CHANCE = doubleKey("monument-heal-chance", 0.2)
        val MONUMENT_HEAL_RADIUS = integerKey("monument-heal-radius", 6)
        val MONUMENT_HEAL_COOLDOWN = doubleKey("monument-heal-cooldown", 1.0)
        val MONUMENT_TIMER_INTERVAL = integerKey("monument-timer-interval", 10)
        val REMOVE_ENTITIES_ON_RESET = booleanKey("remove-entities-on-reset", true)
        val RESET_ON_EMPTY = booleanKey("reset-on-empty", true)
        val CAPTURE_POINT_TIME = integerKey("capture-point-time", 20)
        val SPAWN_PROTECTION_RADIUS = integerKey("spawn-protection-radius", 10)
        val GLOW_FLAG_THIEVES = booleanKey("glow-flag-thieves", true)
    }
}
