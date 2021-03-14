package com.github.james9909.warplus.extensions

import org.bukkit.potion.PotionEffect

fun PotionEffect.format(): String {
    return "${type.name.toLowerCase()}:$amplifier:$duration"
}
