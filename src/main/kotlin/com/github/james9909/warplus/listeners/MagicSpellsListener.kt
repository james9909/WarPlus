package com.github.james9909.warplus.listeners

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.managers.WarParticipant
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import com.nisovin.magicspells.events.SpellCastEvent
import com.nisovin.magicspells.events.SpellTargetEvent
import com.nisovin.magicspells.events.SpellTargetLocationEvent
import com.nisovin.magicspells.spells.targeted.PulserSpell
import org.bukkit.Location
import org.bukkit.entity.Player

class MagicSpellsListener(val plugin: WarPlus) : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onSpellCast(event: SpellCastEvent) {
        if (event.isCancelled) {
            // Apparently we might be receiving a cancelled event despite not opting into
            // ignoreCancelled. Might as well short circuit and validate our assumptions.
            return
        }
        val caster = event.caster
        if (caster !is Player) {
            return
        }

        val playerInfo = plugin.playerManager.getParticipantInfo(caster.uniqueId) ?: return
        when (playerInfo) {
            is WarParticipant.Player -> {
                if (playerInfo.inSpawn) {
                    event.isCancelled = true
                    return
                }
                event.isCancelled = playerInfo.team.warzone.onSpellCast(caster)
            }
            is WarParticipant.Spectator -> {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onSpellTarget(event: SpellTargetEvent) {
        if (event.isCancelled) {
            // Apparently we might be receiving a cancelled event despite not opting into
            // ignoreCancelled. Might as well short circuit and validate our assumptions.
            return
        }

        val caster = event.caster
        if (caster !is Player) {
            return
        }
        val target = event.target
        if (target !is Player) {
            return
        }

        val spell = event.spell
        val targetInfo = plugin.playerManager.getPlayerInfo(target.uniqueId)
        val casterInfo = plugin.playerManager.getPlayerInfo(caster.uniqueId)
        if (casterInfo == null && targetInfo == null) {
            // Neither player is in a warzone, so only allow beneficial spells to target
            if (!spell.isBeneficial) {
                event.isCancelled = true
            }
            return
        } else if (casterInfo == null || targetInfo == null) {
            // One player is not in a warzone
            event.isCancelled = true
            return
        }

        val targetTeam = targetInfo.team
        val casterTeam = casterInfo.team
        if (casterTeam.warzone != targetTeam.warzone) {
            // Only players in the same zone can damage each other
            event.isCancelled = true
            return
        }

        if (targetInfo.inSpawn || casterInfo.inSpawn) {
            event.isCancelled = true
            return
        }

        if (casterTeam == targetTeam) {
            // Only allow beneficial spells to target teammates
            if (!spell.isBeneficial) {
                event.isCancelled = true
            }
        } else {
            // Only allow harmful spells to target enemies
            if (spell.isBeneficial) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onSpellTargetLocationEvent(event: SpellTargetLocationEvent) {
        if (event.isCancelled) {
            // Apparently we might be receiving a cancelled event despite not opting into
            // ignoreCancelled. Might as well short circuit and validate our assumptions.
            return
        }

        if (event.spell !is PulserSpell) {
            return
        }
        val targetLocation: Location = event.targetLocation
        val warzone = plugin.warzoneManager.getWarzones().firstOrNull { warzone ->
            warzone.contains(targetLocation)
        } ?: return
        val realBlock = targetLocation.block
        if (warzone.isSpawnBlock(realBlock) || warzone.onBlockPlace(event.caster, realBlock)) {
            event.isCancelled = true
        }
    }
}
