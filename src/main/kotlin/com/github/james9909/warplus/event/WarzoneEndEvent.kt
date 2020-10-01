package com.github.james9909.warplus.event

import com.github.james9909.warplus.Warzone
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

private val HANDLERS = HandlerList()

class WarzoneEndEvent(val warzone: Warzone) : Event() {
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
