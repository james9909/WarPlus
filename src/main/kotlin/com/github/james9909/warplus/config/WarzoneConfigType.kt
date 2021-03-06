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
        val MONUMENT_EFFECT_RADIUS = integerKey("monument-effect-radius", 6)
        val MONUMENT_TIMER_INTERVAL = integerKey("monument-timer-interval", 80)
        val REMOVE_ENTITIES_ON_RESET = booleanKey("remove-entities-on-reset", true)
        val RESET_ON_EMPTY = booleanKey("reset-on-empty", true)
        val CAPTURE_POINT_TIME = integerKey("capture-point-time", 20)
        val CAPTURE_POINT_TIMER_INTERVAL = integerKey("capture-point-timer-interval", 20)
        val SPAWN_PROTECTION_RADIUS = integerKey("spawn-protection-radius", 10)
        val GLOW_FLAG_THIEVES = booleanKey("glow-flag-thieves", true)
        val GLOW_BOMB_CARRIERS = booleanKey("glow-bomb-carriers", true)
        val RECORD_STATS = booleanKey("record-stats", true)
    }
}
