package com.github.james9909.warplus.listeners

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.WarzoneState
import com.github.james9909.warplus.config.TeamConfigType
import com.github.james9909.warplus.config.WarzoneConfigType
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Firework
import org.bukkit.entity.LightningStrike
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.potion.PotionEffectType

class EntityListener(val plugin: WarPlus) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (event.isCancelled) return

        val defender = event.entity
        val damager = event.damager

        var canFriendlyFire = false // Enable certain damage sources to friendly fire
        val resolvedDamager: Entity? = if (damager is Projectile) {
            when (val shooter = damager.shooter) {
                // Separate the two conditions to take advantage of smart casting
                is Player -> shooter
                is LivingEntity -> shooter
                else -> null
            }
        } else if (damager is TNTPrimed && damager.source is Player) {
            // Attacked by another player's TNT
            canFriendlyFire = true
            damager.source
        } else {
            event.damager
        }

        when (defender) {
            is Player -> handlePlayerDamage(event, defender, resolvedDamager, canFriendlyFire)
            is LivingEntity -> handleMobDamage(event, defender, resolvedDamager)
        }
    }

    private fun handlePlayerDamage(
        event: EntityDamageByEntityEvent,
        defender: Player,
        damager: Entity?,
        canFriendlyFire: Boolean
    ) {
        when (damager) {
            is Player -> handlePlayerDamageByPlayer(event, defender, damager, canFriendlyFire)
            is LivingEntity -> handlePlayerDamageByMonster(event, defender, damager)
            is FallingBlock, is LightningStrike, is Firework, is TNTPrimed, null -> handleNaturalPlayerDamage(event, defender)
            else -> {
                plugin.logger.severe("Failed to explicitly handle damage event:\nevent: $event\ndamager: $damager")
                handleNaturalPlayerDamage(event, defender)
            }
        }
    }

    private fun handlePlayerDamageByPlayer(
        event: EntityDamageByEntityEvent,
        defender: Player,
        damager: Player,
        canFriendlyFire: Boolean
    ) {
        val damagerInfo = plugin.playerManager.getPlayerInfo(damager.uniqueId)
        val defenderInfo = plugin.playerManager.getPlayerInfo(defender.uniqueId)

        if ((damagerInfo == null) xor (defenderInfo == null)) {
            // One is in a warzone while the other is not
            event.isCancelled = true
            return
        }

        if (damagerInfo == null || defenderInfo == null) {
            // Both are not in a warzone
            // NOTE: We use OR for smart-casting, which is fine because
            // the previous conditional takes care of the rest of the cases
            return
        }

        // At this point, both players are in a warzone
        if (damagerInfo.inSpawn || defenderInfo.inSpawn) {
            event.isCancelled = true
            return
        }

        if (damagerInfo.team.warzone != defenderInfo.team.warzone) {
            // Players in different warzones cannot damage each other
            event.isCancelled = true
            return
        }

        if (damagerInfo.team.kind == defenderInfo.team.kind && !canFriendlyFire) {
            // Cancel friendly fire
            event.isCancelled = true
            return
        }

        if (event.finalDamage < defender.health) {
            defenderInfo.lastDamager.damager = damager
            return
        }

        // Player is supposed to die, but cancel the event and respawn them
        event.isCancelled = true
        if (damager == defender) {
            defenderInfo.team.warzone.handleSuicide(defender, event.cause)
        } else {
            defenderInfo.team.warzone.handleKill(damager, defender, damager, event.cause, true)
        }
    }

    private fun handleNaturalPlayerDamage(event: EntityDamageEvent, defender: Player) {
        val defenderInfo = plugin.playerManager.getPlayerInfo(defender.uniqueId) ?: return
        if (defenderInfo.inSpawn) {
            event.isCancelled = true
            return
        }
        if (event.finalDamage < defender.health) return
        event.isCancelled = true
        defenderInfo.team.warzone.handleNaturalDeath(defender, event.cause)
    }

    private fun handlePlayerDamageByMonster(event: EntityDamageByEntityEvent, defender: Player, damager: LivingEntity) {
        // For future reference: handle minion damage
        val defenderInfo = plugin.playerManager.getPlayerInfo(defender.uniqueId) ?: return
        if (defenderInfo.inSpawn) {
            event.isCancelled = true
            return
        }
        if (event.finalDamage < defender.health) return
        event.isCancelled = true
        defenderInfo.team.warzone.handleMobDeath(defender, damager, event.cause)
    }

    private fun handleMobDamage(event: EntityDamageByEntityEvent, defender: LivingEntity, damager: Entity?) {
        when (damager) {
            is Player -> handleMobDamageByPlayer(event, defender, damager)
            null -> {
                // Do nothing
            }
        }
    }

    private fun handleMobDamageByPlayer(event: EntityDamageByEntityEvent, defender: LivingEntity, damager: Player) {
        val damagerInfo = plugin.playerManager.getPlayerInfo(damager.uniqueId) ?: return
        if (damagerInfo.inSpawn) event.isCancelled = true
    }

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        if (event.isCancelled) return
        val originalSize = event.blockList().size
        // Prevent blocks that are important to any warzone from being blown up
        event.blockList().removeIf removeIf@{ block ->
            plugin.warzoneManager.getWarzones().forEach { warzone ->
                if (warzone.contains(block.location)) {
                    // Minor optimization to avoid checking all warzones since we know
                    // that warzones cannot overlap
                    return@removeIf (
                        warzone.state != WarzoneState.RUNNING ||
                            !warzone.warzoneSettings.get(WarzoneConfigType.CAN_BREAK_BLOCKS) ||
                            warzone.isSpawnBlock(block) ||
                            warzone.onBlockBreak(null, block)
                        )
                }
            }
            return@removeIf false
        }

        val newSize = event.blockList().size
        if (newSize != originalSize) {
            // Change explosion yield after explosion protection
            event.yield *= (originalSize - newSize).toFloat() / originalSize
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamage(event: EntityDamageEvent) {
        if (event.isCancelled) return
        if (event is EntityDamageByEntityEvent) return

        val player = event.entity as? Player ?: return
        handleNaturalPlayerDamage(event, player)
    }

    @EventHandler
    fun onEntityRegainHealth(event: EntityRegainHealthEvent) {
        if (event.isCancelled) return
        val player = event.entity as? Player ?: return
        plugin.playerManager.getPlayerInfo(player.uniqueId) ?: return

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
        if (event.isCancelled) return
        val player = event.entity as? Player ?: return
        val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId) ?: return
        if (!playerInfo.team.settings.get(TeamConfigType.HUNGER)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity as? Player ?: return
        plugin.playerManager.getPlayerInfo(player.uniqueId) ?: return

        // Remove drops
        event.drops.clear()
    }

    @EventHandler
    fun onPotionSplash(event: PotionSplashEvent) {
        if (event.isCancelled) return
        val potion = event.potion
        val shooter = potion.shooter

        val beneficial = potion.effects.any {
            it.type == PotionEffectType.WITHER ||
                it.type == PotionEffectType.HARM ||
                it.type == PotionEffectType.POISON ||
                it.type == PotionEffectType.WEAKNESS ||
                it.type == PotionEffectType.BLINDNESS ||
                it.type == PotionEffectType.CONFUSION ||
                it.type == PotionEffectType.HUNGER ||
                it.type == PotionEffectType.SLOW_DIGGING ||
                it.type == PotionEffectType.UNLUCK ||
                it.type == PotionEffectType.SLOW
        }

        if (shooter !is Player) {
            for (entity in event.affectedEntities) {
                if (entity !is Player) {
                    continue
                }
                val warPlayer = plugin.playerManager.getPlayerInfo(entity.uniqueId) ?: continue
                if (warPlayer.inSpawn) {
                    event.setIntensity(entity, 0.0)
                }
            }
        } else {
            val warShooter = plugin.playerManager.getPlayerInfo(shooter.uniqueId)
            if (warShooter != null && warShooter.inSpawn) {
                // Players in spawn can't do anything
                event.isCancelled = true
                return
            }
            for (entity in event.affectedEntities) {
                if (entity !is Player) {
                    continue
                }

                val warPlayer = plugin.playerManager.getPlayerInfo(entity.uniqueId)
                if ((warPlayer == null) xor (warShooter == null)) {
                    // One player is in a warzone, the other isn't
                    event.setIntensity(entity, 0.0)
                    continue
                }
                if (warPlayer == null || warShooter == null) {
                    // Both are not in a warzone
                    // NOTE: We use OR for smart-casting, which is fine because
                    // the previous conditional takes care of the rest of the cases
                    continue
                }
                if (warPlayer.inSpawn) {
                    event.setIntensity(entity, 0.0)
                    continue
                }
                val sameTeam = warPlayer.team == warShooter.team
                if (sameTeam xor beneficial) {
                    // Don't affect teammates with hurtful potions and don't affect enemies with beneficial ones
                    event.setIntensity(entity, 0.0)
                }
            }
        }
    }

    @EventHandler
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        if (event.isCancelled) return
        val player = event.entity as? Player ?: return
        val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId) ?: return
        val warzone = playerInfo.team.warzone

        // Overwrite item
        event.item.itemStack = plugin.itemNameManager.applyItem(event.item.itemStack)
        event.isCancelled = warzone.onPlayerPickupItem(player, event.item)
    }

    @EventHandler
    fun onEntityRegainHealthEvent(event: EntityRegainHealthEvent) {
        if (event.isCancelled) return
        val player = event.entity as? Player ?: return
        plugin.playerManager.getPlayerInfo(player.uniqueId) ?: return
        if (event.regainReason == EntityRegainHealthEvent.RegainReason.REGEN) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEntityChangeBlockEvent(event: EntityChangeBlockEvent) {
        if (event.isCancelled) return
        val block = event.block
        val location = block.location
        val warzone = plugin.warzoneManager.getWarzoneByLocation(location) ?: return
        event.isCancelled = when (event.entityType) {
            EntityType.FALLING_BLOCK -> {
                warzone.isSpawnBlock(block) || warzone.onBlockPlace(event.entity, block)
            }
            else -> {
                warzone.onEntityBlockChange(event.entity, block)
            }
        }
    }
}
