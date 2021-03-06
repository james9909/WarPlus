package com.github.james9909.warplus.util

import com.github.james9909.warplus.WarTeam
import com.github.james9909.warplus.Warzone
import com.google.common.base.Splitter
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criterias
import org.bukkit.scoreboard.DisplaySlot
import java.util.UUID

val PLAYER_SCOREBOARDS: MutableMap<UUID, WarScoreboard> = HashMap()

class WarScoreboard(val player: Player, private val zone: Warzone) {
    val scoreboard = Bukkit.getScoreboardManager()!!.getNewScoreboard()
    private val objective = scoreboard.registerNewObjective("warzone", "dummy", "dummy")
    private var lines = 20
    private var lastFlash = 0L

    init {
        scoreboard.clearSlot(DisplaySlot.SIDEBAR)
        objective.displaySlot = DisplaySlot.SIDEBAR

        // Show player health under their name
        val healthObjective = scoreboard.registerNewObjective("showhealth", Criterias.HEALTH, "${ChatColor.RED}❤${ChatColor.RESET}")
        healthObjective.displaySlot = DisplaySlot.BELOW_NAME

        setTitle("&8>> &6&l${zone.name} &8<<")

        // Add all players to this scoreboard
        zone.teams.forEach { (_, team) ->
            val scoreboardTeam = scoreboard.registerNewTeam(team.getScoreboardName())
            scoreboardTeam.color = team.kind.chatColor
            scoreboardTeam.setCanSeeFriendlyInvisibles(true)
            scoreboardTeam.setAllowFriendlyFire(true) // Enable tnt friendly fire
            team.players.forEach {
                scoreboardTeam.addEntry(it.name)
            }
        }
    }

    fun removePlayer(team: WarTeam, player: Player) {
        val scoreboardTeam = scoreboard.getTeam(team.getScoreboardName())
        scoreboardTeam?.removeEntry(player.name)
    }

    fun addPlayer(team: WarTeam, player: Player) {
        val teamName = team.getScoreboardName()
        val scoreboardTeam = scoreboard.getTeam(teamName) ?: scoreboard.registerNewTeam(teamName)
        scoreboardTeam.addEntry(player.name)
    }

    private fun addTeamText(team: WarTeam, flash: Boolean) {
        val teamName = "&6Team&7: &f$team"
        addText(teamName)

        // Team points
        val teamPoints = "&6Points&7: &e${team.score}&7/&e${team.maxScore()}"
        addText(teamPoints)

        // Lifepool
        val teamLives = "&6Lives&7: &e${team.lives}&7/&e${team.maxLives()}"
        addText(teamLives)
    }

    fun update() {
        this.lines = 20

        addText("")

        // Kill count
        // addText("&6Kills&7: &e0")
        // addText("")

        val now = System.currentTimeMillis()
        val flash = now - lastFlash >= 1000
        zone.teams.values.forEach {
            addTeamText(it, flash)
            addText("")
        }

        if (flash) {
            lastFlash = now
        }
    }

    private fun addText(text: String) {
        addText(text, this.lines--)
    }

    private fun addText(text: String, number: Int) {
        val team = scoreboard.getTeam(number.toString()) ?: {
            val newTeam = scoreboard.registerNewTeam(number.toString())
            val entryName = ChatColor.values()[number].toString()
            newTeam.addEntry(entryName)
            objective.getScore(entryName).score = number
            newTeam
        }()

        val coloredText = ChatColor.translateAlternateColorCodes('&', text)

        // Prefix
        val iterator = Splitter.fixedLength(16).split(coloredText).iterator()
        var prefix = iterator.next()

        // Color behavior adapted from SimpleScoreboard
        var suffix = ""
        if (iterator.hasNext()) {
            var prefixColor = ChatColor.getLastColors(prefix)
            suffix = iterator.next()
            if (prefix.endsWith(ChatColor.COLOR_CHAR.toString())) {
                prefix = prefix.substring(0, prefix.length - 1)
                prefixColor = ChatColor.getByChar(suffix[0]).toString()
                suffix = suffix.substring(1)
            }

            if (suffix.length > 16) {
                suffix = suffix.substring(0, (13 - prefixColor.length))
            }
            suffix = prefixColor + suffix
        }

        team.prefix = prefix
        team.suffix = suffix
    }

    private fun setTitle(text: String) {
        val coloredText = ChatColor.translateAlternateColorCodes('&', text)
        objective.displayName = coloredText
    }

    companion object {
        fun createScoreboard(player: Player, zone: Warzone) {
            val scoreboard = WarScoreboard(player, zone)
            PLAYER_SCOREBOARDS[player.uniqueId] = scoreboard
            player.scoreboard = scoreboard.scoreboard
        }

        fun removeScoreboard(player: Player) {
            PLAYER_SCOREBOARDS.remove(player.uniqueId)
            player.scoreboard = Bukkit.getScoreboardManager()!!.mainScoreboard
        }

        fun getScoreboard(player: Player): WarScoreboard? = PLAYER_SCOREBOARDS[player.uniqueId]
    }
}
