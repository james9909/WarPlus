package com.github.james9909.warplus.util

import com.github.james9909.warplus.extensions.setPotionEffects
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect

const val DEFAULT_MAX_HEALTH = 20.0
const val DEFAULT_EXHAUSTION = 0F
const val DEFAULT_SATURATION = 10F
const val DEFAULT_REMAINING_AIR = 20 * 15
const val DEFAULT_FOOD_LEVEL = 20
const val DEFAULT_EXP = 0F
const val DEFAULT_LEVEL = 0
const val DEFAULT_FALL_DISTANCE = 0F
const val DEFAULT_FIRE_TICKS = 0

data class PlayerState(
    val saturation: Float = DEFAULT_SATURATION,
    val exhaustion: Float = DEFAULT_EXHAUSTION,
    val health: Double = DEFAULT_MAX_HEALTH,
    val maxHealth: Double = DEFAULT_MAX_HEALTH,
    val remainingAir: Int = DEFAULT_REMAINING_AIR,
    val foodLevel: Int = DEFAULT_FOOD_LEVEL,
    val location: Location? = null,
    val gameMode: GameMode = GameMode.SURVIVAL,
    val exp: Float = DEFAULT_EXP,
    val level: Int = DEFAULT_LEVEL,
    val flying: Boolean = false,
    val allowFlight: Boolean = false,
    val fallDistance: Float = DEFAULT_FALL_DISTANCE,
    val fireTicks: Int = DEFAULT_FIRE_TICKS,
    val potionEffects: Collection<PotionEffect>? = null,
    val inventoryContents: Array<ItemStack>? = null
) {

    fun restore(player: Player) {
        player.saturation = saturation
        player.exhaustion = exhaustion
        player.health = health
        player.remainingAir = remainingAir
        player.foodLevel = foodLevel
        player.gameMode = gameMode
        player.exp = exp
        player.level = level
        player.isFlying = flying

        player.setMaxHealth(maxHealth)

        if (inventoryContents != null) {
            player.inventory.contents = inventoryContents
        }
        if (potionEffects != null) {
            player.setPotionEffects(potionEffects)
        }
        if (location != null) {
            player.teleport(location)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerState

        if (saturation != other.saturation) return false
        if (exhaustion != other.exhaustion) return false
        if (health != other.health) return false
        if (maxHealth != other.maxHealth) return false
        if (remainingAir != other.remainingAir) return false
        if (foodLevel != other.foodLevel) return false
        if (location != other.location) return false
        if (gameMode != other.gameMode) return false
        if (exp != other.exp) return false
        if (level != other.level) return false
        if (flying != other.flying) return false
        if (allowFlight != other.allowFlight) return false
        if (fallDistance != other.fallDistance) return false
        if (fireTicks != other.fireTicks) return false
        if (potionEffects != other.potionEffects) return false
        if (inventoryContents != null) {
            if (other.inventoryContents == null) return false
            if (!inventoryContents.contentEquals(other.inventoryContents)) return false
        } else if (other.inventoryContents != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = saturation.hashCode()
        result = 31 * result + exhaustion.hashCode()
        result = 31 * result + health.hashCode()
        result = 31 * result + maxHealth.hashCode()
        result = 31 * result + remainingAir
        result = 31 * result + foodLevel
        result = 31 * result + (location?.hashCode() ?: 0)
        result = 31 * result + gameMode.hashCode()
        result = 31 * result + exp.hashCode()
        result = 31 * result + level
        result = 31 * result + allowFlight.hashCode()
        result = 31 * result + flying.hashCode()
        result = 31 * result + fallDistance.hashCode()
        result = 31 * result + fireTicks
        result = 31 * result + (potionEffects?.hashCode() ?: 0)
        result = 31 * result + (inventoryContents?.contentHashCode() ?: 0)
        return result
    }

    companion object {
        fun fromPlayer(player: Player): PlayerState {
            val maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue ?: DEFAULT_MAX_HEALTH
            return PlayerState(
                player.saturation,
                player.exhaustion,
                player.health,
                maxHealth,
                player.remainingAir,
                player.foodLevel,
                player.location.clone(),
                player.gameMode,
                player.exp,
                player.level,
                player.isFlying,
                player.allowFlight,
                player.fallDistance,
                player.fireTicks,
                player.activePotionEffects,
                player.inventory.contents.clone()
            )
        }
    }
}