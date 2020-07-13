package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.WarzoneState
import com.github.james9909.warplus.command.AbstractCommand
import com.github.james9909.warplus.command.zonemaker.prompts.SetupWarzonePrompt
import org.bukkit.command.CommandSender
import org.bukkit.conversations.Conversation
import org.bukkit.entity.Player

class SetupWarzoneCommand : AbstractCommand() {
    override val USAGE_STRING = "/war setup <name>"
    override val DESCRIPTION = "Setup an existing warzone created with /war create."

    override fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean {
        if (args.isEmpty()) {
            return false
        }
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that")
            return true
        }
        val warzone = plugin.warzoneManager.getWarzone(args[0])
        if (warzone == null) {
            plugin.playerManager.sendMessage(sender, "That warzone doesn't exist")
            return true
        }
        warzone.state = WarzoneState.EDITING
        plugin.inventoryManager.saveInventory(sender)

        val prompt = SetupWarzonePrompt(plugin, sender, warzone)
        val conversation = Conversation(plugin, sender, prompt)
        prompt.conversation = conversation
        conversation.addConversationAbandonedListener(prompt)
        plugin.server.pluginManager.registerEvents(prompt, plugin)
        conversation.isLocalEchoEnabled = false
        conversation.begin()
        return true
    }

    override fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> plugin.warzoneManager.getWarzoneNames().filter {
                it.startsWith(args[0].toLowerCase())
            }
            else -> emptyList()
        }
    }
}