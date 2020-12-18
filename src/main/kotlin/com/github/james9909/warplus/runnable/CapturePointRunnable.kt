package com.github.james9909.warplus.runnable

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.objectives.CapturePointObjective
import com.github.james9909.warplus.objectives.FlagObjective
import org.bukkit.block.BlockFace
import org.bukkit.scheduler.BukkitRunnable

class CapturePointRunnable(private val plugin: WarPlus, private val zone: Warzone) : BukkitRunnable() {

    override fun run() {
        if (!plugin.isEnabled) {
            return
        }
        val objective = zone.objectives["capture_points"] as? CapturePointObjective ?: return
        objective.capturePoints.forEach { cp ->
            zone.teams.forEach { (kind, team) ->
                team.players.forEach { player ->
                    val block = player.location.block.getRelative(BlockFace.DOWN)
                    if (cp.contains(block.location)) {
                        cp.addPlayerForTeam(kind)
                    }
                }
            }
            cp.calculateStrength(zone)
        }
    }
}
