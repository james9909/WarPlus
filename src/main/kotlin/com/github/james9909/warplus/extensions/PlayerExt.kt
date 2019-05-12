package com.github.james9909.warplus.extensions

import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect

fun Player.clearPotionEffects() {
    for (effect in activePotionEffects) {
        removePotionEffect(effect.type)
    }
}

fun Player.setPotionEffects(potionEffects: Collection<PotionEffect>) {
    clearPotionEffects()
    for (effect in potionEffects) {
        addPotionEffect(effect, true)
    }
}