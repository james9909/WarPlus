package com.github.james9909.warplus

import com.github.james9909.warplus.extensions.getOrCreateSection
import com.github.james9909.warplus.extensions.toItemStack
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

typealias RewardItem = Pair<ItemStack, ConfigurationSection?>

class WarReward(val winReward: MutableList<RewardItem>, val lossReward: MutableList<RewardItem>) {
    fun giveWinReward(player: Player) {
        winReward.forEach { item ->
            player.inventory.addItem(item.first)
        }
    }

    fun giveLossReward(player: Player) {
        lossReward.forEach { item ->
            player.inventory.addItem(item.first)
        }
    }

    fun saveConfig(config: ConfigurationSection) {
        val winSection = config.getOrCreateSection("win")
        winReward.forEachIndexed { i, item ->
            if (item.second == null) {
                winSection.set("$i.data", item.first)
            } else {
                winSection.set("$i", item.second)
            }
        }
        val lossSection = config.getOrCreateSection("loss")
        lossReward.forEachIndexed { i, item ->
            if (item.second == null) {
                lossSection.set("$i.data", item.first)
            } else {
                lossSection.set("$i", item.second)
            }
        }
    }

    companion object {
        fun fromConfig(section: ConfigurationSection): WarReward {
            val winSection = section.getConfigurationSection("win")
            val winReward = mutableListOf<RewardItem>()
            winSection?.getKeys(false)?.forEach forEach@{
                val itemSection = winSection.getConfigurationSection(it) ?: return@forEach
                val item = itemSection.toItemStack() ?: return@forEach
                winReward.add(RewardItem(item, itemSection))
            }

            val lossSection = section.getConfigurationSection("loss")
            val lossReward = mutableListOf<RewardItem>()
            lossSection?.getKeys(false)?.forEach forEach@{
                val itemSection = lossSection.getConfigurationSection(it) ?: return@forEach
                val item = itemSection.toItemStack() ?: return@forEach
                lossReward.add(RewardItem(item, itemSection))
            }
            return WarReward(winReward, lossReward)
        }

        fun default(): WarReward = WarReward(mutableListOf(), mutableListOf())
    }
}
