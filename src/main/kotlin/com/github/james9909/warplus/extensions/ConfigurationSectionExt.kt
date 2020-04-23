@file:JvmName("ConfigUtils")

package com.github.james9909.warplus.extensions

import com.github.james9909.warplus.config.ConfigKey
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.LeatherArmorMeta

val materialMap by lazy {
    val materialMap = mutableMapOf<String, Material>()
    for (material in Material.values()) {
        materialMap[material.name.toLowerCase()] = material
    }
    materialMap
}

fun ConfigurationSection.getOrCreateSection(path: String): ConfigurationSection {
    return getConfigurationSection(path) ?: createSection(path)
}

fun ConfigurationSection.getLocation(path: String): Location? {
    if (!isString(path)) {
        return null
    }

    return getString(path)?.toLocation()
}

fun ConfigurationSection.getLocationList(path: String): List<Location> {
    val locations = mutableListOf<Location>()
    getStringList(path).forEach {
        locations.add(it.toLocation())
    }
    return locations
}

fun <T> ConfigurationSection.get(key: ConfigKey<T>): T = key.get(this)

fun <T> ConfigurationSection.get(key: ConfigKey<T>, default: T): T = key.get(this, default)

fun handleLore(itemMeta: ItemMeta, path: ConfigurationSection) {
    if (path.isList("lore")) {
        val lore = path.getStringList("lore")
        lore.map {
            it.color()
        }
        itemMeta.lore = lore
    } else if (path.isString("lore")) {
        val lore = path.getString("lore")
        if (lore != null) itemMeta.lore = mutableListOf(lore.color())
    }
}

fun handleEnchants(itemMeta: ItemMeta, path: ConfigurationSection) {
    if (!path.isList("enchants")) {
        return
    }

    val enchants = path.getStringList("enchants")
    enchants.forEach {
        val split = it.split(":")
        if (split.size != 2) {
            println("Invalid enchant: $it")
            return@forEach
        }

        val key = NamespacedKey.minecraft(split[0].toLowerCase())
        val level = split[1].toIntOrNull()
        if (level == null) {
            println("Invalid level: ${split[1]}")
            return@forEach
        }

        val enchant = Enchantment.getByKey(key)
        if (enchant == null) {
            println("Invalid enchant: $key")
            return@forEach
        }
        if (itemMeta is EnchantmentStorageMeta) {
            itemMeta.addStoredEnchant(enchant, level, true)
        } else {
            itemMeta.addEnchant(enchant, level, true)
        }
    }
}

fun handleColor(itemMeta: ItemMeta, path: ConfigurationSection) {
    if (itemMeta !is LeatherArmorMeta) {
        return
    }
    val color = path.getString("color") ?: return
    val hex = color.removePrefix("#").toIntOrNull(16)
    if (hex == null) {
        println("Invalid color: $color")
        return
    }
    itemMeta.setColor(Color.fromRGB(hex))
}

fun ConfigurationSection.toItemStack(): ItemStack? {
    if (contains("data")) {
        // Support spigot ItemStack format
        // See: https://www.spigotmc.org/wiki/itemstack-serialization/
        return getItemStack("data")
    }
    if (!contains("type")) {
        println("No type specified for $name")
        return null
    }
    val type = materialMap[getString("type")]
    if (type == null) {
        println("Invalid type '${getString("type")}")
        return null
    }

    val item = ItemStack(type, getInt("amount", 1))
    val meta = item.itemMeta ?: return null
    val name = getString("name")
    if (name != null) {
        meta.setDisplayName(name.color())
    }

    handleLore(meta, this)
    handleEnchants(meta, this)
    handleColor(meta, this)

    item.itemMeta = meta
    return item
}
