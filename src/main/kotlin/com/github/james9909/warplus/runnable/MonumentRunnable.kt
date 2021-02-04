package com.github.james9909.warplus.runnable

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.config.WarzoneConfigType
import com.github.james9909.warplus.objectives.MonumentObjective
import com.github.james9909.warplus.util.NANOSECONDS_PER_SECOND
import java.util.UUID
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import org.bukkit.attribute.Attribute
import org.bukkit.scheduler.BukkitRunnable

class MonumentRunnable(private val plugin: WarPlus, private val zone: Warzone) : BukkitRunnable() {
    private val lastHealed = hashMapOf<UUID, Long>()

    @ExperimentalTime
    override fun run() {
        if (!plugin.isEnabled) {
            this.cancel()
            return
        }

        val obj = zone.objectives["monuments"] as? MonumentObjective ?: return
        val healDist = zone.warzoneSettings.get(WarzoneConfigType.MONUMENT_HEAL_RADIUS).toDouble().pow(2)
        if (healDist < 1.0) return

        val healAmount = zone.warzoneSettings.get(WarzoneConfigType.MONUMENT_HEAL)
        val healChance = zone.warzoneSettings.get(WarzoneConfigType.MONUMENT_HEAL_CHANCE)
        val healCooldown = zone.warzoneSettings.get(WarzoneConfigType.MONUMENT_HEAL_COOLDOWN)
        val zoneMaxHealth = zone.warzoneSettings.get(WarzoneConfigType.MAX_HEALTH)
        zone.teams.forEach { (kind, team) ->
            team.players.forEach { player ->
                obj.monuments.forEach forMonument@{ monument ->
                    if (monument.owner != kind || player.location.distanceSquared(monument.origin) > healDist) {
                        return@forMonument
                    }

                    val maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue ?: zoneMaxHealth
                    val currHealth = player.health
                    if (currHealth >= maxHealth || Random.Default.nextDouble() >= healChance) {
                        return@forMonument
                    }
                    val now = System.nanoTime()
                    if (now - lastHealed.getOrDefault(player.uniqueId, 0) < healCooldown * NANOSECONDS_PER_SECOND) {
                        return@forMonument
                    }

                    val newHealth = min(maxHealth, currHealth + healAmount)
                    player.health = newHealth
                    plugin.playerManager.sendMessage(player, "Your team's monument buffs you!")
                    lastHealed[player.uniqueId] = now
                }
            }
        }
    }
}
