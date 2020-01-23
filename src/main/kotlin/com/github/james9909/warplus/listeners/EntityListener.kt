package com.github.james9909.warplus.listeners

import com.github.james9909.warplus.WarPlus
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.TNTPrimed
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
        val defenderInfo = plugin.playerManager.getPlayerInfo(defender) ?: return
        if (defenderInfo.inSpawn) {
            event.isCancelled = true
            return
        }

        val attacker = if (damager is Projectile) {
            when (damager.shooter) {
                is Player -> damager.shooter
                is LivingEntity -> damager.shooter
                else -> null
            }
        } else if (damager is TNTPrimed && damager.source is Player) {
            // Attacked by another player's TNT
            damager.source
        } else {
            event.damager
        }

        when (attacker) {
            is Player -> handlePvP(event, attacker, defender)
            is LivingEntity -> handlePvE(event, attacker, defender)
            null -> handleNaturalDamage(event, defender)
        }
    }

    private fun handleNaturalDamage(event: EntityDamageByEntityEvent, defender: Player) {
        val defenderInfo = plugin.playerManager.getPlayerInfo(defender) ?: return
        if (event.finalDamage < defender.health) {
            return
        }
        event.isCancelled = true
        defenderInfo.team.warzone.handleNaturalDeath(defender, event.cause)
    }

    private fun handlePvE(event: EntityDamageByEntityEvent, attacker: LivingEntity, defender: Player) {
        val defenderInfo = plugin.playerManager.getPlayerInfo(defender) ?: return
        if (event.finalDamage < defender.health) {
            return
        }
        event.isCancelled = true
        defenderInfo.team.warzone.handleMobDeath(defender, attacker)
    }

    private fun handlePvP(event: EntityDamageByEntityEvent, attacker: Player, defender: Player) {
        val attackerInfo = plugin.playerManager.getPlayerInfo(attacker)
        val defenderInfo = plugin.playerManager.getPlayerInfo(defender)

        if ((attackerInfo == null) xor (defenderInfo == null)) {
            // One is in a warzone while the other is not
            event.isCancelled = true
            return
        }
        if (attackerInfo == null || defenderInfo == null) {
            // Both are not in a warzone
            // NOTE: We use OR for smart-casting, which is fine because
            // the previous conditional takes care of the rest of the cases
            return
        }

        // At this point, both players are in a warzone

        if (attackerInfo.team.warzone != defenderInfo.team.warzone) {
            // Players in different warzones cannot damage each other
            event.isCancelled = true
            return
        }

        if (attackerInfo.team.kind == defenderInfo.team.kind) {
            // Cancel friendly fire
            event.isCancelled = true
            return
        }

        if (event.finalDamage < defender.health) {
            return
        }

        if (attacker == defender) {
            defenderInfo.team.warzone.handleSuicide(defender)
        } else {
            defenderInfo.team.warzone.handleKill(attacker, defender, attacker, true)
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
        val playerInfo = plugin.playerManager.getPlayerInfo(player) ?: return
        if (playerInfo.inSpawn) {
            event.isCancelled = true
            return
        }

        if (event.finalDamage < player.health) {
            return
        }

        event.isCancelled = true
        playerInfo.team.warzone.handleNaturalDeath(player, event.cause)
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
        val shooter = potion.shooter

        var beneficial = true
        val effects = potion.effects
        loop@ for (effect in effects) {
            when (effect.type) {
                PotionEffectType.HARM, PotionEffectType.POISON, PotionEffectType.WEAKNESS, PotionEffectType.SLOW -> {
                    beneficial = false
                    break@loop
                }
            }
        }

        if (shooter !is Player) {
            for (entity in event.affectedEntities) {
                if (entity !is Player) {
                    continue
                }
                val warPlayer = plugin.playerManager.getPlayerInfo(entity) ?: continue
                if (warPlayer.inSpawn) {
                    event.setIntensity(entity, 0.0)
                }
            }
        } else {
            val warShooter = plugin.playerManager.getPlayerInfo(shooter)
            if (warShooter != null && warShooter.inSpawn) {
                // Players in spawn can't do anything
                event.isCancelled = true
                return
            }
            for (entity in event.affectedEntities) {
                if (entity !is Player) {
                    continue
                }

                val warPlayer = plugin.playerManager.getPlayerInfo(entity)
                if (warPlayer == null && warShooter == null) {
                    // Outside of scope
                    continue
                }
                if (warPlayer == null || warShooter == null) {
                    // One player is in a warzone, the other isn't
                    event.setIntensity(entity, 0.0)
                    continue
                }
                if (warPlayer.inSpawn) {
                    event.setIntensity(entity, 0.0)
                    continue
                }
                val sameTeam = warPlayer.team == warShooter.team
                if (sameTeam && !beneficial) {
                    // Don't affect teammates with hurtful potions
                    event.setIntensity(entity, 0.0)
                }
            }
        }
    }
}