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
        fun apply(item: ItemStack) {
            if (type != null) {
                item.type = type
            }
            val meta = item.itemMeta
            if (name != null) {
                meta?.setDisplayName(name)
            }
            if (lore != null) {
                meta?.lore = lore
            }
            item.itemMeta = meta
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

    private fun loadItemNames(config: ConfigurationSection) {
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

    fun applyItem(item: ItemStack): ItemStack {
        val mod = itemNames[item.type] ?: return item
        return when (mod) {
            is ItemModification.CustomItemStack -> {
                mod.customItem
            }
            is ItemModification.Meta -> {
                mod.apply(item)
                item
            }
        }
    }
}
