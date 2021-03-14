package com.github.james9909.warplus.command.zonemaker.prompts

import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.WarzoneState
import com.github.james9909.warplus.extensions.blockLocation
import com.github.james9909.warplus.extensions.color
import com.github.james9909.warplus.extensions.isFinite
import com.github.james9909.warplus.objectives.defaultMonumentEffects
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.bukkit.Material
import org.bukkit.block.Block
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

private enum class TOOL(val display: String) {
    CORNERS("Corners"),
    SPAWN("Spawns"),
    FLAG("Flags"),
    MONUMENT("Monuments"),
    CAPTURE_POINT("Capture Points"),
    BOMB("Bombs")
}

class SetupWarzonePrompt(val plugin: WarPlus, val player: Player, val warzone: Warzone) : Prompt,
    ConversationAbandonedListener, Listener {
    private var text = "Enter \"done\" to finish editing."
    private var teamKind: TeamKind? = null
    lateinit var conversation: Conversation

    private var cornerOneSet = warzone.region.p1.isFinite()
    private var cornerTwoSet = warzone.region.p2.isFinite()

    init {
        player.inventory.apply {
            clear()
            contents = TOOLS
            heldItemSlot = 0
        }
    }

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
        return when {
            input == "done" -> handleDone()
            input.startsWith("team ") -> handleTeam(input)
            input == "help" -> handleHelp()
            else -> handleInvalid()
        }
    }

    private fun handleDone(): Prompt? {
        if (::conversation.isInitialized) {
            player.abandonConversation(conversation)
        }
        return Prompt.END_OF_CONVERSATION
    }

    private fun handleInvalid(): Prompt {
        text = "Invalid input. Type \"help\" for help"
        return this
    }

    private fun handleTeam(input: String): Prompt {
        val team = input.split(" ")[1].trim()
        try {
            teamKind = TeamKind.valueOf(team.toUpperCase())
            text = "Team set to $team"
        } catch (e: IllegalArgumentException) {
            text = "Invalid team kind $team"
        }
        return this
    }

    private fun handleHelp(): Prompt {
        val sb = StringBuffer()
        sb.append("Help:\n")
        sb.append("help: Show this message\n")
        sb.append("team <team color>: Set the team to create structures for\n")
        sb.append("done: Save and quit\n")
        text = sb.toString()
        return this
    }

    private fun isTool(item: ItemStack): Boolean {
        if (item.type == Material.AIR) {
            return false
        }

        return TOOL.values().any {
            item.itemMeta?.displayName == it.display
        }
    }

    override fun conversationAbandoned(abandonedEvent: ConversationAbandonedEvent) {
        HandlerList.unregisterAll(this)

        if (cornerOneSet && cornerTwoSet) {
            player.sendMessage("Saving warzone ${warzone.name}...")
            if (warzone.pruneStructures()) {
                player.sendMessage("Structures that are no longer within the warzone have been removed")
            }
            warzone.saveVolume()
            warzone.saveConfig()
            player.sendMessage("Setup complete!")
            warzone.state = WarzoneState.IDLING
        } else {
            player.sendMessage("Setup incomplete. Missing points.")
        }
        plugin.inventoryManager.restoreInventory(player)
    }

    @EventHandler
    private fun onBlockBreakEvent(event: BlockBreakEvent) {
        if (event.player != player) return
        player.inventory.itemInMainHand.let {
            if (!isTool(it)) {
                return
            }
            event.isCancelled = true
        }
    }

    @EventHandler
    private fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.player != player) return
        if (event.hand == EquipmentSlot.OFF_HAND) return
        if (event.item == null) return
        event.item?.let {
            if (!isTool(it)) {
                return
            }
            event.isCancelled = true
            when (it.itemMeta?.displayName) {
                TOOL.CORNERS.display -> if (event.hasBlock()) {
                    handleCorners(
                        event.action,
                        event.clickedBlock!! // Guaranteed not to be null by the check above
                    )
                }
                TOOL.SPAWN.display -> handleSpawn(event)
                TOOL.FLAG.display -> handleFlag(event)
                TOOL.MONUMENT.display -> handleMonument(event)
                TOOL.CAPTURE_POINT.display -> handleCapturePoint(event)
                TOOL.BOMB.display -> handleBomb(event)
            }
        }
        player.sendRawMessage(getPromptText(conversation.context))
    }

    @EventHandler
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        if (event.player == player && player.isConversing) {
            player.abandonConversation(conversation)
        }
    }

    @EventHandler
    private fun onPluginDisable(event: PluginDisableEvent) {
        if (event.plugin.name == plugin.name && player.isConversing) {
            player.abandonConversation(conversation)
        }
    }

    private fun handleCorners(action: Action, clickedBlock: Block) {
        when (action) {
            Action.LEFT_CLICK_BLOCK -> {
                warzone.region.p1 = clickedBlock.location
                cornerOneSet = true
                text = "Corner 1 set"
            }
            Action.RIGHT_CLICK_BLOCK -> {
                warzone.region.p2 = clickedBlock.location
                cornerTwoSet = true
                text = "Corner 2 set"
            }
            else -> {
                text = "Please click on a block"
            }
        }
    }

    private fun handleSpawn(event: PlayerInteractEvent) {
        val location = player.location
        val spawn = warzone.teams.values.flatMap { it.spawns }.firstOrNull {
            it.contains(location)
        }
        when (event.action) {
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
                // Add spawn
                if (spawn != null) {
                    text = "A spawn already exists here"
                    return
                }
                val currTeamKind = teamKind
                if (currTeamKind == null) {
                    text = "A team has not been set yet. Input \"team <team>\" to set the team."
                    return
                }

                val origin = player.location.subtract(0.0, 1.0, 0.0).blockLocation()
                text = when (val result = warzone.addTeamSpawn(origin, currTeamKind)) {
                    is Ok -> "Spawn for team ${currTeamKind.name.toLowerCase()} created!"
                    is Err -> result.error.toString()
                }
            }
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
                if (spawn == null) {
                    text = "There is no spawn here"
                    return
                }
                // Remove spawn
                spawn.restoreVolume()
                warzone.removeTeamSpawn(spawn)
                warzone.saveConfig()
                text = "Spawn removed!"
            }
            else -> {
                // Do nothing
            }
        }
    }

    private fun handleFlag(event: PlayerInteractEvent) {
        val location = player.location
        when (event.action) {
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
                val currTeamKind = teamKind
                if (currTeamKind == null) {
                    text = "A team has not been set yet. Input \"team <team>\" to set the team."
                    return
                }
                val team = warzone.teams[currTeamKind]
                if (team == null) {
                    text = "This team doesn't exist. Please create a spawn first"
                    return
                }

                val origin = location.subtract(0.0, 1.0, 0.0).blockLocation()
                text = when (val result = warzone.addFlagObjective(origin, team.kind)) {
                    is Ok -> "Flag for team ${currTeamKind.name.toLowerCase()} created!"
                    is Err -> result.error.toString()
                }
            }
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
                val flag = warzone.getFlagAtLocation(location)
                if (flag == null) {
                    text = "There is no flag here"
                    return
                }
                flag.restoreVolume()
                warzone.removeFlag(flag)
                warzone.saveConfig()
                text = "Flag removed!"
            }
            else -> {
                // Do nothing
            }
        }
    }

    private fun handleMonument(event: PlayerInteractEvent) {
        val location = player.location
        when (event.action) {
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
                val origin = location.subtract(0.0, 1.0, 0.0).blockLocation()
                text = when (val result = warzone.addMonumentObjective(origin, "Monument", defaultMonumentEffects)) {
                    is Ok -> "Monument created! Change its name in the config."
                    is Err -> result.error.toString()
                }
            }
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
                val monument = warzone.getMonumentAtLocation(location)
                if (monument == null) {
                    text = "There is no monument here"
                    return
                }
                monument.restoreVolume()
                warzone.removeMonument(monument)
                warzone.saveConfig()
                text = "Monument removed!"
            }
            else -> {
                // Do nothing
            }
        }
    }

    private fun handleCapturePoint(event: PlayerInteractEvent) {
        val location = player.location
        when (event.action) {
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
                val origin = location.subtract(0.0, 1.0, 0.0).blockLocation()
                text = when (val result = warzone.addCapturePointObjective(origin, "Capture Point")) {
                    is Ok -> "Capture point created! Change its name in the config."
                    is Err -> result.error.toString()
                }
            }
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
                val cp = warzone.getCapturePointAtLocation(location)
                if (cp == null) {
                    text = "There is no capture point here"
                    return
                }
                cp.restoreVolume()
                warzone.removeCapturePoint(cp)
                warzone.saveConfig()
                text = "Capture point removed!"
            }
            else -> {
                // Do nothing
            }
        }
    }

    private fun handleBomb(event: PlayerInteractEvent) {
        val location = player.location
        when (event.action) {
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
                val origin = location.subtract(0.0, 1.0, 0.0).blockLocation()
                text = when (val result = warzone.addBombObjective(origin, "Bomb")) {
                    is Ok -> "Bomb created! Change its name in the config."
                    is Err -> result.error.toString()
                }
            }
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
                val bomb = warzone.getBombAtLocation(location)
                if (bomb == null) {
                    text = "There is no bomb here"
                    return
                }
                bomb.restoreVolume()
                warzone.removeBomb(bomb)
                warzone.saveConfig()
                text = "Bomb removed!"
            }
            else -> {
                // Do nothing
            }
        }
    }

    companion object {
        private val TOOLS = arrayOf(
            createTool(Material.WOODEN_AXE, TOOL.CORNERS, "Set corner 1", "Set corner 2"),
            createTool(Material.WOODEN_SWORD, TOOL.SPAWN, "Add spawn", "Remove spawn"),
            createTool(Material.WOODEN_HOE, TOOL.FLAG, "Create flag", "Remove flag"),
            createTool(Material.WOODEN_PICKAXE, TOOL.MONUMENT, "Create monument", "Remove monument"),
            createTool(Material.WOODEN_SHOVEL, TOOL.CAPTURE_POINT, "Create capture point", "Remove capture point"),
            createTool(Material.FLINT_AND_STEEL, TOOL.BOMB, "Create bomb", "Remove bomb")
        )

        private fun createTool(material: Material, name: TOOL, left: String, right: String): ItemStack {
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
