package com.github.james9909.warplus.util

import org.bukkit.entity.Player

const val SECONDS_TIL_EXPIRATION = 5
const val NANOSECONDS_PER_SECOND = 1_000_000_000.0

class LastDamager(initialDamager: Player?) {
    var damager: Player? = initialDamager
        set(value) {
            field = value
            timestamp = System.nanoTime()
        }
        get() {
            val damager = field
            if (damager != null &&
                System.nanoTime() - timestamp < SECONDS_TIL_EXPIRATION * NANOSECONDS_PER_SECOND) {
                return damager
            }
            return null
        }
    private var timestamp = System.nanoTime()
}
