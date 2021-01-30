package com.github.james9909.warplus.runnable

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.config.TeamConfigType
import com.github.james9909.warplus.config.WarzoneConfigType
import com.github.james9909.warplus.objectives.CapturePointObjective
import com.github.james9909.warplus.structures.CapturePointState
import org.bukkit.block.BlockFace
import org.bukkit.scheduler.BukkitRunnable

class CapturePointRunnable(private val plugin: WarPlus, private val zone: Warzone) : BukkitRunnable() {

    override fun run() {
        if (!plugin.isEnabled) {
            this.cancel()
            return
        }
        val objective = zone.objectives["capture_points"] as? CapturePointObjective ?: return
        val capturePointTime = zone.warzoneSettings.get(WarzoneConfigType.CAPTURE_POINT_TIME)
        objective.capturePoints.forEach { cp ->
            cp.clearActiveTeams()
            if (zone.teams.values.count { it.hasEnoughPlayers() } == 1) return
            zone.teams.forEach { (kind, team) ->
                team.players.forEach { player ->
                    val block = player.location.block.getRelative(BlockFace.DOWN)
                    if (cp.contains(block.location)) {
                        cp.addPlayerForTeam(kind)
                    }
                }
            }
            cp.calculateStrength(zone)
            val state = cp.state
            if (state is CapturePointState.Captured) {
                state.controlTime += 1
                if (state.controlTime == capturePointTime) {
                    val team = zone.teams[state.controller]!!
                    team.addPoint()
                    zone.broadcast("Team $team gained 1 point for maintaining control of capture point ${cp.name}")

                    // Detect win condition
                    if (team.score >= team.settings.get(TeamConfigType.MAX_SCORE)) {
                        team.warzone.handleWin(listOf(team.kind))
                        return@forEach
                    }
                    state.controlTime = 0
                }
            }
        }
    }
}
