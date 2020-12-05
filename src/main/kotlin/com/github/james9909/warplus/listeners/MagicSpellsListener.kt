package com.github.james9909.warplus.listeners

import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.managers.WarParticipant
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import com.nisovin.magicspells.events.SpellCastEvent
import com.nisovin.magicspells.events.SpellTargetEvent
import org.bukkit.entity.Player

class MagicSpellsListener(val plugin: WarPlus) : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onSpellCast(event: SpellCastEvent) {
        val caster = event.caster
        if (caster !is Player) {
            return
        }

        val playerInfo = plugin.playerManager.getParticipantInfo(caster) ?: return
        when (playerInfo) {
            is WarParticipant.Player -> {
                if (playerInfo.inSpawn) {
                    event.isCancelled = true
                    return
                }
                playerInfo.team.warzone.onSpellCast(caster)
            }
            is WarParticipant.Spectator -> {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onSpellTarget(event: SpellTargetEvent) {
        val caster = event.caster
        if (caster !is Player) {
            return
        }
        val target = event.target
        if (target !is Player) {
            return
        }

        val spell = event.spell
        val targetInfo = plugin.playerManager.getPlayerInfo(target)
        val casterInfo = plugin.playerManager.getPlayerInfo(caster)
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
}
