package com.github.james9909.warplus.config

import com.github.james9909.warplus.structures.SpawnStyle

class TeamConfigType private constructor() {
    companion object {
        val DEFAULT_CLASS = stringKey("default-class", "")
        val ECON_REWARD = doubleKey("econ-reward", 5.0)
        val LIVES = integerKey("lives", 20)
        val HUNGER = booleanKey("hunger", true)
        val MAX_PLAYERS = integerKey("max-players", 20)
        val MAX_SCORE = integerKey("max-score", 2)
        val MIN_PLAYERS = integerKey("min-players", 1)
        val PLACE_BLOCKS = booleanKey("place-blocks", true)
        val SPAWN_STYLE = enumKey("spawn-style", SpawnStyle.SMALL)
    }
}
