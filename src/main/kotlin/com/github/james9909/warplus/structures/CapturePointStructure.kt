package com.github.james9909.warplus.structures

import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.config.TeamConfigType
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockState
import kotlin.random.Random

private fun removeRandomBlock(blocks: MutableList<BlockState>): BlockState? {
    if (blocks.isEmpty()) {
        return null
    }

    val index = Random.nextInt(blocks.size)
    return blocks.removeAt(index)
}

sealed class CapturePointState {
    object Neutral : CapturePointState()
    data class Captured(val controller: TeamKind, var controlTime: Int) : CapturePointState()
    data class Contested(val contestant: TeamKind, val controller: TeamKind?) : CapturePointState()
}

class CapturePointStructure(
    plugin: WarPlus,
    origin: Location,
    val name: String
) : AbstractStructure(plugin, origin) {
    override val prefix = "zone/capture_points"
    override val corners by lazy {
        val topLeft = origin.block
            .getRelative(orientation.front.toBlockFace(), 4)
            .getRelative(orientation.left.toBlockFace(), 4)
        val bottomRight = origin.block
            .getRelative(orientation.back.toBlockFace(), 4)
            .getRelative(orientation.right.toBlockFace(), 4)
            .getRelative(BlockFace.UP)
        Pair(topLeft.location, bottomRight.location)
    }
    private val neutralBlocks = mutableListOf<BlockState>()
    private val coloredBlocks = mutableListOf<BlockState>()
    private val activeTeams = hashMapOf<TeamKind, Int>()
    var state: CapturePointState = CapturePointState.Neutral
        private set

    fun reset() {
        activeTeams.clear()
        neutralBlocks.clear()
        coloredBlocks.clear()
        state = CapturePointState.Neutral
    }

    fun clearActiveTeams() { activeTeams.clear() }

    fun addPlayerForTeam(team: TeamKind) {
        activeTeams[team] = activeTeams.getOrDefault(team, 0) + 1
    }

    private fun addNeutralBlock() {
        val targetBlock = removeRandomBlock(coloredBlocks)
        if (targetBlock != null) {
            targetBlock.type = Material.SMOOTH_STONE
            targetBlock.update(true)
            neutralBlocks.add(targetBlock)
        }
    }

    private fun addColoredBlock(kind: TeamKind) {
        val targetBlock = removeRandomBlock(neutralBlocks)
        if (targetBlock != null) {
            targetBlock.type = kind.material
            targetBlock.update(true)
            coloredBlocks.add(targetBlock)
        }
    }

    private fun incrementStrengthFromNeutral(zone: Warzone, majority: TeamKind) {
        zone.broadcast("Team ${majority.format()} is gaining control of point $name!")
        addColoredBlock(majority)
        state = CapturePointState.Contested(majority, null)
    }

    private fun decrementStrengthFromCaptured(zone: Warzone, majority: TeamKind, controller: TeamKind) {
        zone.broadcast("Capture point $name is being contested by team ${majority.format()}!")
        addNeutralBlock()
        state = CapturePointState.Contested(majority, controller)
    }

    private fun decrementStrengthFromContested(zone: Warzone, controller: TeamKind?) {
        addNeutralBlock()
        if (coloredBlocks.isEmpty()) {
            // Capture point has been neutralized
            state = CapturePointState.Neutral
            if (controller != null) {
                zone.broadcast("Team ${controller.format()} has lost control of capture point $name.")
            }
        }
    }

    private fun fortifyPointAsController(zone: Warzone, controller: TeamKind) {
        addColoredBlock(controller)
        if (neutralBlocks.isEmpty()) {
            // Capture point has strengthened itself. No points are awarded as this is passive
            state = CapturePointState.Captured(controller, 0)
        }
    }

    private fun incrementStrengthAsContestant(zone: Warzone, contestant: TeamKind) {
        addColoredBlock(contestant)
        if (neutralBlocks.isEmpty()) {
            // Capture point has been captured
            zone.broadcast("Team ${contestant.format()} has captured point $name and gained 1 point.")
            state = CapturePointState.Captured(contestant, 0)

            val team = zone.teams[contestant]!!
            team.addPoint()
            // Detect win condition
            if (team.score >= team.settings.get(TeamConfigType.MAX_SCORE)) {
                zone.handleWin(listOf(team.kind))
            }
        }
    }

    fun calculateStrength(zone: Warzone) {
        val majority = activeTeams.maxByOrNull { it.value }
        val totalPower = activeTeams.values.sum()

        when (val currState = state) {
            is CapturePointState.Neutral -> {
                if (majority != null && majority.value > totalPower / 2.0) {
                    // A team is taking the point
                    incrementStrengthFromNeutral(zone, majority.key)
                }
            }
            is CapturePointState.Contested -> {
                if (majority == null) {
                    if (currState.controller == null) {
                        // Bring the point back down to neutral
                        decrementStrengthFromContested(zone, null)
                    }
                } else if (majority.value <= totalPower / 2.0) {
                    // Nobody has the majority
                    when {
                        currState.controller == null -> {
                            // Nobody holds the majority on a previously neutral point, so bring it down
                            decrementStrengthFromContested(zone, null)
                        }
                        activeTeams.getOrDefault(currState.controller, 0) < totalPower / 2.0 -> {
                            // Controller has a strict minority, lose control of the point
                            decrementStrengthFromContested(zone, currState.controller)
                        }
                        else -> {
                            // Equal power, so do nothing
                        }
                    }
                } else {
                    // A team has the strict majority
                    if (majority.key == currState.controller) {
                        // Controller takes back control
                        fortifyPointAsController(zone, currState.controller)
                    } else {
                        // A challenger approaches!
                        if (majority.key == currState.contestant) {
                            if (currState.controller == null) {
                                // Contesting from a neutral point
                                incrementStrengthAsContestant(zone, majority.key)
                            } else {
                                // Continue strengthening the point as the current challenger
                                decrementStrengthFromContested(zone, currState.controller)
                            }
                        } else {
                            // New challenger!
                            decrementStrengthFromContested(zone, currState.controller)
                        }
                    }
                }
            }
            is CapturePointState.Captured -> {
                if (majority != null && majority.value > totalPower / 2.0) {
                    if (majority.key != currState.controller) {
                        // Contest point as the new majority
                        decrementStrengthFromCaptured(zone, majority.key, currState.controller)
                    }
                }
            }
        }
    }

    override fun getStructure(): Array<Array<Array<Material>>> {
        val obsidian = Material.OBSIDIAN
        val stone = Material.SMOOTH_STONE
        val none = Material.FIRE
        return arrayOf(
            arrayOf(
                arrayOf(none, none, none, none, obsidian, none, none, none, none),
                arrayOf(none, none, obsidian, obsidian, obsidian, obsidian, obsidian, none, none),
                arrayOf(none, obsidian, obsidian, stone, stone, stone, obsidian, obsidian, none),
                arrayOf(none, obsidian, stone, stone, stone, stone, stone, obsidian, none),
                arrayOf(obsidian, obsidian, stone, stone, stone, stone, stone, obsidian, obsidian),
                arrayOf(none, obsidian, stone, stone, stone, stone, stone, obsidian, none),
                arrayOf(none, obsidian, obsidian, stone, stone, stone, obsidian, obsidian, none),
                arrayOf(none, none, obsidian, obsidian, obsidian, obsidian, obsidian, none, none),
                arrayOf(none, none, none, none, obsidian, none, none, none, none)
            )
        )
    }

    override fun postBuild() {
        val structure = getStructure()
        val (topLeft, _) = corners
        structure.forEachIndexed { yOffset, layer ->
            val blockY = topLeft.block.getRelative(BlockFace.UP, yOffset)
            layer.forEachIndexed { zOffset, row ->
                val blockYZ = blockY.getRelative(orientation.back.toBlockFace(), zOffset)
                row.forEachIndexed { xOffset, material ->
                    val blockXYZ = blockYZ.getRelative(orientation.right.toBlockFace(), xOffset)
                    if (material == Material.SMOOTH_STONE) {
                        neutralBlocks.add(blockXYZ.state)
                    }
                }
            }
        }
    }
}
