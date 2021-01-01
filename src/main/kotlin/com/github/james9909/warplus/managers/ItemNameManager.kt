package com.github.james9909.warplus.managers

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.extensions.color
import com.github.james9909.warplus.extensions.toItemStack
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import java.io.File

val materialMap by lazy {
    val materialMap = mutableMapOf<String, Material>()
    for (material in Material.values()) {
        materialMap[material.name.toLowerCase()] = material
    }
    materialMap
}

sealed class ItemModification {
    data class Meta(val type: Material?, val name: String?, val lore: List<String>?) : ItemModification() {
        fun apply(item: ItemStack): Boolean {
            var modified = false
            if (type != null) {
                modified = item.type != type
                item.type = type
            }
            val meta = item.itemMeta
            if (name != null) {
                modified = modified || (meta != null && meta.displayName != name)
                meta?.setDisplayName(name)
            }
            if (lore != null) {
                modified = modified || (meta != null && meta.lore != lore)
                meta?.lore = lore
            }
            item.itemMeta = meta
            return modified
        }

        companion object {
            fun fromConfig(config: ConfigurationSection): Meta {
                val type = config.getString("type")?.run {
                    materialMap[this]
                }
                val name = config.getString("name")?.color()
                val lore = config.getStringList("lore").map { it.color() }
                return Meta(type, name, lore)
            }
        }
    }

    data class CustomItemStack(val customItem: ItemStack) : ItemModification() {
        fun apply(item: ItemStack): Boolean {
            if (item == customItem) {
                return false
            }
            item.type = customItem.type
            item.itemMeta = customItem.itemMeta
            item.data = customItem.data
            item.amount = customItem.amount
            item.enchantments.clear()
            item.enchantments.forEach { (enchant, _) ->
                item.removeEnchantment(enchant)
            }
            customItem.enchantments.forEach { (enchant, level) ->
                item.addEnchantment(enchant, level)
            }
            return true
        }

        companion object {
            fun fromConfig(config: ConfigurationSection): CustomItemStack? {
                val item = config.toItemStack() ?: return null
                return CustomItemStack(item)
            }
        }
    }

    companion object {
        fun fromConfig(section: ConfigurationSection): ItemModification? {
            return when (section.get("resolver")) {
                "item" -> {
                    CustomItemStack.fromConfig(section)
                }
                else -> {
                    Meta.fromConfig(section)
                }
            }
        }
    }
}

class ItemNameManager(private val plugin: WarPlus) {
    private val itemNames = mutableMapOf<Material, ItemModification>()

    fun loadItemNames() {
        val itemNameFile = File(plugin.dataFolder, "item-drops.yml")
        if (!itemNameFile.exists()) {
            plugin.saveResource("item-drops.yml", true)
        }
        loadItemNames(YamlConfiguration.loadConfiguration(itemNameFile))
    }

    fun loadItemNames(config: ConfigurationSection) {
        config.getKeys(false).forEach { materialName ->
            val material = materialMap[materialName.toLowerCase()]
            if (material != null) {
                val section = config.getConfigurationSection(materialName)!!
                val itemMod = ItemModification.fromConfig(section)
                if (itemMod != null) {
                    itemNames[material] = itemMod
                } else {
                    println("Failed to parse item for $materialName")
                }
            } else {
                println("Invalid item name: $materialName")
            }
        }
    }

    fun clear() = itemNames.clear()

    fun applyItem(item: ItemStack): Boolean {
        val mod = itemNames[item.type] ?: return false
        return when (mod) {
            is ItemModification.CustomItemStack -> {
                mod.apply(item)
            }
            is ItemModification.Meta -> {
                mod.apply(item)
            }
        }
    }
}
