package com.github.james9909.warplus.runnable

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.config.WarzoneConfigType
import com.github.james9909.warplus.objectives.MonumentObjective
import kotlin.math.pow
import org.bukkit.scheduler.BukkitRunnable

class MonumentRunnable(private val plugin: WarPlus, private val zone: Warzone) : BukkitRunnable() {
    override fun run() {
        if (!plugin.isEnabled) {
            this.cancel()
            return
        }

        val obj = zone.objectives["monuments"] as? MonumentObjective ?: return
        val radius = zone.warzoneSettings.get(WarzoneConfigType.MONUMENT_EFFECT_RADIUS).toDouble().pow(2)
        if (radius < 1.0) return

        zone.teams.forEach { (kind, team) ->
            team.players.forEach { player ->
                obj.monuments.forEach forMonument@{ monument ->
                    if (monument.owner != kind || player.location.distanceSquared(monument.origin) > radius) {
                        return@forMonument
                    }

                    player.addPotionEffects(monument.potionEffects)
                    plugin.playerManager.sendMessage(player, "Your team's monument buffs you!")
                }
            }
        }
    }
}
