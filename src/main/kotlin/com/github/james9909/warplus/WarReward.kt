package com.github.james9909.warplus

import com.github.james9909.warplus.extensions.getOrCreateSection
import com.github.james9909.warplus.extensions.toItemStack
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

typealias RewardItem = Pair<ItemStack, ConfigurationSection?>

private fun writeToSection(section: ConfigurationSection, rewards: List<RewardItem>) {
    rewards.forEachIndexed { i, item ->
        if (item.second == null) {
            section.set("$i.data", item.first)
        } else {
            section.set("$i", item.second)
        }
    }
}

private fun readFromSection(section: ConfigurationSection?): List<RewardItem> {
    val rewards = mutableListOf<RewardItem>()
    section?.getKeys(false)?.forEach forEach@{
        val itemSection = section.getConfigurationSection(it) ?: return@forEach
        val item = itemSection.toItemStack() ?: return@forEach
        rewards.add(RewardItem(item, itemSection))
    }
    return rewards
}

class WarReward(val winReward: MutableList<RewardItem>, val lossReward: MutableList<RewardItem>, val mvpReward: MutableList<RewardItem>) {
    fun giveWinReward(player: Player) = giveReward(player, winReward)

    fun giveLossReward(player: Player) = giveReward(player, lossReward)

    fun giveMvpReward(player: Player) = giveReward(player, mvpReward)

    fun saveConfig(config: ConfigurationSection) {
        writeToSection(config.getOrCreateSection("win"), winReward)
        writeToSection(config.getOrCreateSection("loss"), lossReward)
        writeToSection(config.getOrCreateSection("mvp"), mvpReward)
    }

    private fun giveReward(player: Player, items: List<RewardItem>) {
        items.forEach { item ->
            player.inventory.addItem(item.first)
        }
    }

    companion object {
        fun fromConfig(section: ConfigurationSection): WarReward {
            val winReward = readFromSection(section.getConfigurationSection("win")).toMutableList()
            val lossReward = readFromSection(section.getConfigurationSection("loss")).toMutableList()
            val mvpReward = readFromSection(section.getConfigurationSection("mvp")).toMutableList()
            return WarReward(winReward, lossReward, mvpReward)
        }

        fun default(): WarReward = WarReward(mutableListOf(), mutableListOf(), mutableListOf())
    }
}
