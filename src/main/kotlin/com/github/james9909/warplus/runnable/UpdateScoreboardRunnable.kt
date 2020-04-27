package com.github.james9909.warplus.runnable

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.util.PLAYER_SCOREBOARDS
import org.bukkit.scheduler.BukkitRunnable

class UpdateScoreboardRunnable(val plugin: WarPlus): BukkitRunnable() {
    override fun run() {
        if (!plugin.isEnabled) {
            return
        }
        PLAYER_SCOREBOARDS.forEach { _, scoreboard ->
            scoreboard.update()
        }
    }
}