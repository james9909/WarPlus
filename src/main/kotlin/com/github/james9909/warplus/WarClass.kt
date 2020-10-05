package com.github.james9909.warplus

import com.github.james9909.warplus.extensions.format
import com.github.james9909.warplus.extensions.getLocationFromString
import com.github.james9909.warplus.extensions.toItemStack
import org.bukkit.Location
import org.bukkit.block.Chest
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.TreeMap

data class ArmorSet(
    val helmet: ItemStack?,
    val chestplate: ItemStack?,
    val leggings: ItemStack?,
    val boots: ItemStack?
) {
    companion object {
        fun default(): ArmorSet {
            return ArmorSet(null, null, null, null)
        }
    }
}

data class WarClass(
    val name: String,
    val offhand: ItemStack?,
    val items: Map<Int, ItemStack>,
    val armor: ArmorSet,
    var classchest: Location?
) {

    fun giveToPlayer(player: Player) {
        player.inventory.clear()

        val cc = classchest
        if (cc == null) {
            giveToPlayer(player, offhand, items, armor)
        } else {
            val state = cc.block.state
            if (state is Chest) {
                giveToPlayer(player, state.blockInventory)
            }
        }
    }

    private fun giveToPlayer(player: Player, inventory: Inventory) {
        val contents: Array<ItemStack?> = inventory.contents
        val length = contents.size
        val items = contents.copyOfRange(0, length - 5).toList()
        val offhand = contents[length - 5]
        val armor = contents.copyOfRange(length - 4, length)

        val itemMap = TreeMap<Int, ItemStack>()
        items.forEachIndexed forEach@{ index, item ->
            if (item != null) {
                itemMap[index] = item
            }
        }
        giveToPlayer(
            player, offhand, itemMap, ArmorSet(
                armor[3],
                armor[2],
                armor[1],
                armor[0]
            )
        )
    }

    private fun giveToPlayer(player: Player, offhand: ItemStack?, items: Map<Int, ItemStack>, armor: ArmorSet) {
        player.inventory.apply {
            items.forEach { (index, item) ->
                setItem(index, item)
            }
            if (offhand != null) {
                setItemInOffHand(offhand)
            }
            helmet = armor.helmet
            chestplate = armor.chestplate
            leggings = armor.leggings
            boots = armor.boots
        }
    }

    fun saveConfig(section: ConfigurationSection) {
        val itemsSection = section.createSection("items")
        items.forEach { (index, item) ->
            itemsSection.set("$index.data", item)
        }
        section.set("offhand.data", offhand)
        section.set("helmet.data", armor.helmet)
        section.set("chestplate.data", armor.chestplate)
        section.set("leggings.data", armor.leggings)
        section.set("boots.data", armor.boots)
        val cc = classchest
        if (cc != null) {
            section.set("classchest", cc.format())
        }
    }

    companion object {
        fun fromConfig(name: String, section: ConfigurationSection): WarClass {
            val itemSection = section.getConfigurationSection("items")
            val itemMap: MutableMap<Int, ItemStack> = TreeMap()
            itemSection?.getKeys(false)?.forEach forEach@{
                val item = itemSection.getConfigurationSection(it)?.toItemStack() ?: return@forEach
                itemMap[Integer.parseInt(it)] = item
            }
            val offhand = section.getConfigurationSection("offhand")?.toItemStack()
            return WarClass(
                name,
                offhand,
                itemMap,
                ArmorSet(
                    section.getConfigurationSection("helmet")?.toItemStack(),
                    section.getConfigurationSection("chestplate")?.toItemStack(),
                    section.getConfigurationSection("leggings")?.toItemStack(),
                    section.getConfigurationSection("boots")?.toItemStack()
                ),
                section.getLocationFromString("classchest")
            )
        }
    }
}
