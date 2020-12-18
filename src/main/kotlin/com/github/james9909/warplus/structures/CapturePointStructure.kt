package com.github.james9909.warplus.structures

import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockState

class CapturePointStructure(
    plugin: WarPlus,
    origin: Location,
    val name: String,
    private val maxStrength: Int,
) : AbstractStructure(plugin, origin) {
    override val prefix = "zone/capture_points"
    override val corners by lazy {
        val topLeft = origin.block
            .getRelative(orientation.front.toBlockFace(), 4)
            .getRelative(orientation.left.toBlockFace(), 4)
        val bottomRight = origin.block
            .getRelative(orientation.back.toBlockFace(), 4)
            .getRelative(orientation.right.toBlockFace(), 4)
        Pair(topLeft.location, bottomRight.location)
    }
    private val coloredBlocks = mutableSetOf<BlockState>()
    private val activeTeams = hashMapOf<TeamKind, Int>()
    private var controller: TeamKind? = null
    private var strength = 0;

    fun reset() {
        coloredBlocks.clear()
        controller = null
    }

    fun addPlayerForTeam(team: TeamKind) {
        activeTeams[team] = activeTeams.getOrDefault(team, 0) + 1
    }

    private fun decrementStrength(zone: Warzone, majority: TeamKind?) {
        strength -= 1
        if (strength <= 0) {
            if (controller == null) {
            }
        }
    }

    private fun incrementStrength(zone: Warzone, majority: TeamKind) {
        if (strength == maxStrength) {
            // We're done here.
        }
    }

    fun calculateStrength(zone: Warzone) {
        val majority = activeTeams.maxBy { it.value }
        if (majority == null) {
            // Nobody is here, so decrement the strength
            if (controller == null) {
                decrementStrength(zone, null)
            }
        } else {
            val totalPower = activeTeams.values.sum()
            if (majority.value >= totalPower / 2.0) {
                // We have a strict majority
                if (controller == majority.key) {
                    // Fortify our point

                } else {
                    // Contest enemy point as the majority
                    decrementStrength(zone, majority.key)
                }
            } else {
                if (controller != majority.key) {
                    decrementStrength(zone, null)
                }
            }
        }
    }

    override fun getStructure(): Array<Array<Array<Material>>> {
        val obsidian = Material.OBSIDIAN
        val stone = Material.SMOOTH_STONE
        val air = Material.AIR
        return arrayOf(
            arrayOf(
                arrayOf(air, air, air, air, obsidian, air, air, air, air),
                arrayOf(air, air, obsidian, obsidian, obsidian, obsidian, obsidian, air, air),
                arrayOf(air, obsidian, obsidian, stone, stone, stone, obsidian, obsidian, air),
                arrayOf(air, obsidian, stone, stone, stone, stone, stone, obsidian, air),
                arrayOf(obsidian, obsidian, stone, stone, stone, stone, stone, obsidian, obsidian),
                arrayOf(air, obsidian, stone, stone, stone, stone, stone, obsidian, air),
                arrayOf(air, obsidian, obsidian, stone, stone, stone, obsidian, obsidian, air),
                arrayOf(air, air, obsidian, obsidian, obsidian, obsidian, obsidian, air, air),
                arrayOf(air, air, air, air, obsidian, air, air, air, air)
            )
        )
    }
}
