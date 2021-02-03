package com.github.james9909.warplus.extensions

import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect

fun Player.clearPotionEffects() = activePotionEffects.forEach { effect ->
    removePotionEffect(effect.type)
}

fun Player.setPotionEffects(potionEffects: Collection<PotionEffect>) {
    clearPotionEffects()
    potionEffects.forEach { addPotionEffect(it) }
}
