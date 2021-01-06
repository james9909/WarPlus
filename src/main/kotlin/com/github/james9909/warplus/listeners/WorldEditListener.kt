package com.github.james9909.warplus.listeners

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.WarzoneState
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.event.extent.EditSessionEvent
import com.sk89q.worldedit.extent.MaskingExtent
import com.sk89q.worldedit.function.mask.Mask
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.util.eventbus.Subscribe
import org.bukkit.Location
import org.bukkit.World

class WarzoneMask(private val plugin: WarPlus, private val world: World) : Mask {
    override fun test(vector: BlockVector3): Boolean {
        val location = Location(world, vector.x.toDouble(), vector.y.toDouble(), vector.z.toDouble())
        val warzone = plugin.warzoneManager.getWarzones().firstOrNull { warzone ->
            warzone.contains(location)
        } ?: return true
        if (warzone.state == WarzoneState.RUNNING) {
            // We only care about potentially malicious pastes if the warzone is currently running
            val realBlock = world.getBlockAt(location)
            return !warzone.isSpawnBlock(realBlock) && !warzone.onBlockPlace(null, realBlock)
        }
        return true
    }

    override fun copy(): Mask = WarzoneMask(plugin, world)
}

class FaweListener(private val plugin: WarPlus) {
    @Subscribe
    fun onEditSessionEvent(event: EditSessionEvent) {
        val world = event.world
        if (world != null) {
            event.extent.addProcessor(
                MaskingExtent(
                    event.extent,
                    WarzoneMask(plugin, BukkitAdapter.adapt(world))
                )
            )
        }
    }
}
