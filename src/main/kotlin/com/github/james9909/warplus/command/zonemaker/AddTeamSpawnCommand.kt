package com.github.james9909.warplus.command.zonemaker

import com.github.james9909.warplus.WarTeam
import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.command.AbstractCommand
import com.github.james9909.warplus.config.TeamConfigType
import com.github.james9909.warplus.extensions.blockLocation
import com.github.james9909.warplus.extensions.get
import com.github.james9909.warplus.structure.SpawnStyle
import com.github.james9909.warplus.structure.TeamSpawnStructure
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AddTeamSpawnCommand(plugin: WarPlus, sender: CommandSender, args: List<String>) :
    AbstractCommand(plugin, sender, args) {
    override fun handle(): Boolean {
        if (args.isEmpty()) {
            return false
        }
        if (sender !is Player) {
            plugin.playerManager.sendMessage(sender, "Only in-game players may do that")
            return true
        }
        val warzone = plugin.warzoneManager.getWarzoneByLocation(sender.location)
        if (warzone == null) {
            plugin.playerManager.sendMessage(sender, "You're not in a warzone")
            return true
        }
        val kind: TeamKind
        try {
            kind = TeamKind.valueOf(args[0].toUpperCase())
        } catch (e: IllegalArgumentException) {
            plugin.playerManager.sendMessage(sender, "Invalid team ${args[0]}")
            return true
        }
        var team = warzone.teams[kind]
        if (team == null) {
            team = WarTeam(kind, mutableListOf(), warzone)
            warzone.addTeam(team)
        }
        val spawnStyle: SpawnStyle
        try {
            spawnStyle = team.settings.get(TeamConfigType.SPAWN_STYLE)
        } catch (e: java.lang.IllegalArgumentException) {
            plugin.playerManager.sendMessage(sender, "Invalid spawn style ${team.settings.getString("spawn-style")}")
            return true
        }
        val teamSpawn =
            TeamSpawnStructure(plugin, sender.location.subtract(0.0, 1.0, 0.0).blockLocation(), team.kind, spawnStyle).also {
                it.saveVolume()
                it.build()
            }
        team.addTeamSpawn(teamSpawn)
        warzone.saveConfig()
        plugin.playerManager.sendMessage(sender, "Spawn for team ${args[0]} created!")
        return true
    }
}