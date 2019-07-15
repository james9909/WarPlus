package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.Team
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.WarzoneState
import com.github.james9909.warplus.command.AbstractCommand
import com.github.james9909.warplus.extensions.color
import com.github.james9909.warplus.region.Region
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.conversations.Conversation
import org.bukkit.conversations.ConversationAbandonedEvent
import org.bukkit.conversations.ConversationAbandonedListener
import org.bukkit.conversations.ConversationContext
import org.bukkit.conversations.Prompt
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class SetupWarzoneCommand(plugin: WarPlus, sender: CommandSender, args: List<String>) :
    AbstractCommand(plugin, sender, args) {

    override fun handle(): Boolean {
        if (args.isEmpty()) {
            return false
        }
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that")
            return true
        }
        val warzone = plugin.warzoneManager.getWarzone(args[0]) ?: Warzone(
            plugin,
            args[0],
            Region(sender.world)
        )
        warzone.state = WarzoneState.EDITING
        val prompt = SetupWarzonePrompt(sender)
        val conversation = Conversation(plugin, sender, prompt)
        val conversationListener = SetupWarzoneListener(plugin, sender, warzone, conversation, prompt)
        prompt.conversation = conversation
        conversation.addConversationAbandonedListener(conversationListener)
        plugin.server.pluginManager.registerEvents(conversationListener, plugin)
        conversation.isLocalEchoEnabled = false
        conversation.begin()
        return true
    }

    private class SetupWarzonePrompt(val player: Player) : Prompt {
        var text = "Enter \"quit\" to finish editing"
        var action: (input: String) -> Unit = {}
        lateinit var conversation: Conversation

        override fun blocksForInput(context: ConversationContext): Boolean {
            return true
        }

        override fun getPromptText(context: ConversationContext): String {
            return "[WarPlus] $text"
        }

        override fun acceptInput(context: ConversationContext, input: String?): Prompt? {
            if (input == null) {
                return this
            }
            if (input == "quit") {
                if (::conversation.isInitialized) {
                    player.abandonConversation(conversation)
                    text = "Exiting setup"
                    return this
                }
            }
            action(input)
            action = {}
            return this
        }
    }

    private class SetupWarzoneListener(
        val plugin: WarPlus,
        val player: Player,
        val warzone: Warzone,
        val conversation: Conversation,
        val prompt: SetupWarzonePrompt
    ) : ConversationAbandonedListener, Listener {
        enum class TOOL_NAME(val display: String) {
            CORNERS("Corners"),
            SPAWN("Spawns"),
            OBJECTIVES("Objectives")
        }

        init {
            player.inventory.apply {
                clear()
                contents = TOOLS
                heldItemSlot = 0
            }
        }

        fun isTool(item: ItemStack): Boolean {
            if (item.type == Material.AIR) {
                return false
            }

            return TOOL_NAME.values().any {
                item.itemMeta?.displayName == it.display
            }
        }

        override fun conversationAbandoned(abandonedEvent: ConversationAbandonedEvent) {
            HandlerList.unregisterAll(this)

            player.sendMessage("Saving warzone ${warzone.name}...")
            warzone.save()
        }

        @EventHandler
        fun onBlockBreakEvent(event: BlockBreakEvent) {
            if (event.player != player) return
            player.inventory.itemInMainHand.let {
                if (!isTool(it)) {
                    return
                }
                event.isCancelled = true
            }
        }

        @EventHandler
        fun onPlayerInteract(event: PlayerInteractEvent) {
            if (event.player != player) return
            if (event.hand == EquipmentSlot.OFF_HAND) return
            if (event.item == null) return
            if (!event.hasBlock()) return
            event.isCancelled = true
            event.item?.let {
                if (!isTool(it)) {
                    return
                }
                when (it.itemMeta?.displayName) {
                    TOOL_NAME.CORNERS.display -> setCorners(event.action, event.clickedBlock!!) // clickedBlock must be non-null, as guaranteed by the conditions above
                    TOOL_NAME.SPAWN.display -> handleSpawn(event)
                }
            }
        }

        @EventHandler
        fun onPlayerQuit(event: PlayerQuitEvent) {
            if (event.player == player && player.isConversing) {
                player.abandonConversation(conversation)
            }
        }

        @EventHandler
        fun onPluginDisable(event: PluginDisableEvent) {
            if (event.plugin.name == plugin.name && player.isConversing) {
                player.abandonConversation(conversation)
            }
        }

        fun setCorners(action: Action, clickedBlock: Block) {
            when (action) {
                Action.LEFT_CLICK_BLOCK -> {
                    warzone.region.p1 = clickedBlock.location
                    player.sendRawMessage("P1 set")
                }
                Action.RIGHT_CLICK_BLOCK -> {
                    warzone.region.p2 = clickedBlock.location
                    player.sendRawMessage("P2 set")
                }
                else -> {
                    // Do nothing
                }
            }
        }

        fun handleSpawn(event: PlayerInteractEvent) {
            val block = event.clickedBlock ?: return
            val location = block.location
            val spawn = warzone.teams.flatMap { it.spawns }.firstOrNull {
                it.blockX == location.blockX && it.blockY == location.blockY && it.blockZ == location.blockZ
            }
            when (event.action) {
                Action.LEFT_CLICK_BLOCK -> {
                    // Add spawn
                    if (spawn != null) {
                        player.sendRawMessage("A spawn already exists at that location")
                        return
                    }
                    prompt.text = "Input the team name:"
                    prompt.action = { s: String ->
                        val team = warzone.teams.firstOrNull {
                            it.name == s
                        } ?: Team(s, mutableListOf(), warzone)
                        team.spawns.add(location)
                        prompt.text = "Spawn created!"
                    }
                }
                Action.RIGHT_CLICK_BLOCK -> {
                    // Remove spawn
                }
                else -> {
                }
            }
        }

        companion object {
            val TOOLS = arrayOf(
                createTool(Material.WOODEN_AXE, TOOL_NAME.CORNERS, "Set p1", "Set p2"),
                createTool(Material.WOODEN_SWORD, TOOL_NAME.SPAWN, "Add spawn", "Remove spawn"),
                createTool(Material.WOODEN_HOE, TOOL_NAME.OBJECTIVES, "Create objective", "Remove objective")
            )

            fun createTool(material: Material, name: TOOL_NAME, left: String, right: String): ItemStack {
                val tool = ItemStack(material)
                val meta = tool.itemMeta
                meta?.apply {
                    setDisplayName(name.display)
                    lore = mutableListOf(
                        "&9Left&r: &r $left".color(),
                        "&cRight&r: &r $right".color()
                    )
                    tool.itemMeta = meta
                }
                return tool
            }
        }
    }
}