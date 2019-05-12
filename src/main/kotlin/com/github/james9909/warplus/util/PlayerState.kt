package com.github.james9909.warplus.util

import com.github.james9909.warplus.DEFAULT_MAX_HEALTH
import com.github.james9909.warplus.extensions.setPotionEffects
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player

data class PlayerState(val player: Player) {
    private var saturation = player.saturation
    private var exhaustion = player.exhaustion
    private var health = player.health
    private var maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue ?: DEFAULT_MAX_HEALTH
    private var foodLevel = player.foodLevel
    private var location = player.location
    private var gameMode = player.gameMode
    private var exp = player.exp
    private var level = player.level
    private var flying = player.isFlying
    private var potionEffects = player.activePotionEffects

    fun update() {
        saturation = player.saturation
        exhaustion = player.exhaustion
        health = player.health
        maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue ?: DEFAULT_MAX_HEALTH
        foodLevel = player.foodLevel
        location = player.location
        gameMode = player.gameMode
        exp = player.exp
        level = player.level
        flying = player.isFlying
    }

    fun restore() {
        player.saturation = saturation
        player.exhaustion = exhaustion
        player.health = health
        player.foodLevel = foodLevel
        player.gameMode = gameMode
        player.exp = exp
        player.level = level
        player.isFlying = flying

        player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue ?: DEFAULT_MAX_HEALTH
        player.setPotionEffects(potionEffects)
        player.teleport(location)
    }
}