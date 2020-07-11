package com.github.james9909.warplus.config

class WarzoneConfigType {
    companion object {
        val BLOCK_HEADS = booleanKey("block-heads", true)
        val CLASS_CMD = stringKey("class-cmd", "")
        val DEATH_MESSAGES = booleanKey("death-messages", true)
        val ENABLED = booleanKey("enabled", true)
        val MAX_HEALTH = doubleKey("max-health", 20.0)
        val MIN_TEAMS = integerKey("min-teams", 2)
        val ITEM_DROPS = booleanKey("item-drops", false)
        val MONUMENT_HEAL = integerKey("monument-heal", 1)
        val MONUMENT_HEAL_CHANCE = doubleKey("monument-heal-chance", 0.2)
        val REMOVE_ENTITIES_ON_RESET = booleanKey("remove-entities-on-reset", true)
        val RESET_ON_EMPTY = booleanKey("reset-on-empty", true)
    }
}
