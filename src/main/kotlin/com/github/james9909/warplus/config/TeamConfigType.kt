package com.github.james9909.warplus.config

import com.github.james9909.warplus.structure.SpawnStyle

class TeamConfigType {
    companion object {
        val LIVES = integerKey("lives", 20)
        val MIN_PLAYERS = integerKey("min-players", 1)
        val MAX_PLAYERS = integerKey("max-players", 20)
        val MAX_SCORE = integerKey("max-score", 2)
        val SPAWN_STYLE = enumKey("spawn-style", SpawnStyle.SMALL)
    }
}
