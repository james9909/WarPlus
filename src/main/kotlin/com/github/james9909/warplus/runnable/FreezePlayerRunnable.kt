package com.github.james9909.warplus.runnable

import com.github.james9909.warplus.util.NANOSECONDS_PER_SECOND
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

const val TICKS_PER_SECOND = 20

class FreezePlayerRunnable(
    private val player: Player,
    private val location: Location,
    private val duration: Long,
    private val start: Long = System.nanoTime()
) : BukkitRunnable() {
    override fun run() {
        if (System.nanoTime() - start >= (duration / TICKS_PER_SECOND) * NANOSECONDS_PER_SECOND) {
            this.cancel()
            return
        }
        player.teleport(location)
    }
}
