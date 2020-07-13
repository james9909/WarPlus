package com.github.james9909.warplus.util

import org.bukkit.entity.Player

const val SECONDS_TIL_EXPIRATION = 5

class LastDamager(_damager: Player?) {
    var damager: Player? = _damager
        set(value) {
            field = value
            timestamp = System.nanoTime()
        }
        get() {
            val damager = field
            if (damager != null && ((System.nanoTime() - timestamp) / 1_000_000_000.0) < SECONDS_TIL_EXPIRATION) {
                return damager
            }
            return null
        }
    private var timestamp = System.nanoTime()
}