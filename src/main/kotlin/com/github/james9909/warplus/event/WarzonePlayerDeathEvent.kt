package com.github.james9909.warplus.event

import com.github.james9909.warplus.Warzone
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.EntityDamageEvent.DamageCause

private val HANDLERS = HandlerList()

class WarzonePlayerDeathEvent(
    val player: Player,
    val warzone: Warzone,
    val entity: Entity?,
    val cause: DamageCause
) : Event() {
    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

    companion object {
        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }
}