package com.github.james9909.warplus.listeners

import com.github.james9909.warplus.WarPlus
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.potion.PotionEffectType

class EntityListener(val plugin: WarPlus) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val defender = event.entity as? Player ?: return
        val damager = event.damager

        val attacker: Entity
        if (damager is Projectile && damager.shooter is Player) {
            attacker = damager.shooter as Player
        } else {
            attacker = event.damager
        }

        val warDefender = plugin.playerManager.getPlayerInfo(defender) ?: return
        if (attacker is Player) {
            // PVP
            val warAttacker = plugin.playerManager.getPlayerInfo(attacker)
            if (warAttacker?.team == null) {
                return
            }

            val aTeam = warAttacker.team
            val dTeam = warDefender.team
            if (aTeam.warzone != dTeam.warzone) {
                // Same warzone, different teams
                event.isCancelled = true
                return
            }
            if (aTeam == dTeam) {
                // No friendly fire
                event.isCancelled = true
                return
            }
            if (event.finalDamage < defender.health) {
                return
            }

            // Death
            event.isCancelled = true
            if (attacker == defender) {
                // suicide
            } else {
                // kill
            }
            return
        }

        if (attacker is LivingEntity) {
            // PVE
            val isDeath = event.finalDamage >= defender.health
            if (!isDeath) {
                return
            }
            // Kill
        }
    }

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        plugin.warzoneManager.getWarzoneByLocation(event.location) ?: return
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamage(event: EntityDamageEvent) {
        if (event is EntityDamageByEntityEvent) {
            return
        }

        val player = event.entity as? Player ?: return
        plugin.playerManager.getPlayerInfo(player) ?: return

        if (event.finalDamage < player.health) {
            return
        }

        event.isCancelled = true
        // Handle natural kill
    }

    @EventHandler
    fun onEntityRegainHealth(event: EntityRegainHealthEvent) {
        val player = event.entity as? Player ?: return
        plugin.playerManager.getPlayerInfo(player) ?: return

        when (event.regainReason) {
            EntityRegainHealthEvent.RegainReason.REGEN -> {
                // Disable peaceful mode regen
                event.isCancelled = true
            }
            else -> {
            }
        }
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        // TODO: Implement me
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity as? Player ?: return
        plugin.playerManager.getPlayerInfo(player) ?: return

        // Remove drops
        event.drops.clear()
    }

    @EventHandler
    fun onPotionSplash(event: PotionSplashEvent) {
        val potion = event.potion
        val shooter = potion.shooter as? Player ?: return

        val warShooter = plugin.playerManager.getPlayerInfo(shooter) ?: return
        var beneficial = true
        val effects = potion.effects
        for (effect in effects) {
            when (effect.type) {
                PotionEffectType.HARM, PotionEffectType.POISON, PotionEffectType.WEAKNESS, PotionEffectType.SLOW -> {
                    beneficial = false
                }
            }
        }

        for (entity in event.affectedEntities) {
            if (entity !is Player) {
                continue
            }

            val warPlayer = plugin.playerManager.getPlayerInfo(entity)
            if (warPlayer == null) {
                // Don't affect outsiders
                event.setIntensity(entity, 0.0)
            } else {
                val sameTeam = warPlayer.team == warShooter.team
                if (sameTeam && !beneficial) {
                    // Don't affect teammates with hurtful potions
                    event.setIntensity(entity, 0.0)
                }
            }
        }
    }
}