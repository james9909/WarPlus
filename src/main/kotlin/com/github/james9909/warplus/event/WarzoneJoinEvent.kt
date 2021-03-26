package com.github.james9909.warplus.event

import com.github.james9909.warplus.Warzone
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

private val HANDLERS = HandlerList()

class WarzoneJoinEvent(val player: Player, val warzone: Warzone) : Cancellable, Event() {
    private var cancelled = false

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

    companion object {
        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }
}
