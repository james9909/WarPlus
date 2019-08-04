package com.github.james9909.warplus.extensions

import org.bukkit.Location

fun Location.format(): String {
    // Format: world:x,y,z[,[yaw],[pitch]]
    return "${this.world?.name}:${this.x},${this.y},${this.z},${this.yaw},${this.pitch}"
}
