package com.github.james9909.warplus.managers

import com.github.james9909.warplus.Err
import com.github.james9909.warplus.IllegalWarzoneException
import com.github.james9909.warplus.Ok
import com.github.james9909.warplus.Team
import com.github.james9909.warplus.WarException
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.extensions.LocationFormatException
import com.github.james9909.warplus.extensions.getLocation
import com.github.james9909.warplus.extensions.getLocationList
import com.github.james9909.warplus.extensions.getOrCreateSection
import com.github.james9909.warplus.region.Region
import com.github.kittinunf.result.Result
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration

class WarzoneManager(val plugin: WarPlus) {
    private val warzones = mutableMapOf<String, Warzone>()

    fun loadWarzones() {
        plugin.dataFolder.listFiles()?.forEach {
            System.out.println(it)
            if (!it.name.startsWith("warzone-") || it.extension != "yml") {
                return
            }
            val name = it.nameWithoutExtension.substring(8)
            plugin.logger.info("Loading zone $name")
            val result = loadWarzone(name, YamlConfiguration.loadConfiguration(it))
            when (result) {
                is Ok -> {
                    warzones[name.toLowerCase()] = result.value
                    plugin.logger.info("Loaded zone $name")
                }
                is Err -> plugin.logger.warning("Failed to load warzone $name: ${result.error}")
            }
        }
    }

    fun loadWarzone(
        name: String,
        config: ConfigurationSection
    ): Result<Warzone, WarException> {
        val infoSection = config.getOrCreateSection("info")
        val zoneSettings = config.getOrCreateSection("settings")
        val teamSettings = config.getOrCreateSection("team-settings")
        val teamsSection = config.getOrCreateSection("teams")

        // Get region information
        val worldName = infoSection.getString("world") ?: plugin.server.worlds[0].name
        val world = plugin.server.getWorld(worldName) ?: plugin.server.worlds[0]

        val p1: Location
        val p2: Location
        try {
            p1 = infoSection.getLocation("p1") ?: return Err(
                IllegalWarzoneException(
                    "No p1 defined"
                )
            )
            p2 = infoSection.getLocation("p2") ?: return Err(
                IllegalWarzoneException(
                    "No p2 defined"
                )
            )
        } catch (e: LocationFormatException) {
            return Err(
                IllegalWarzoneException(
                    "Invalid p1 or p2"
                )
            )
        }

        val region = Region(world, p1, p2)
        val warzone = Warzone(
            plugin = plugin,
            name = name,
            region = region,
            teamSettings = teamSettings,
            warzoneSettings = zoneSettings
        )

        // Get teams
        val teamNames = teamsSection.getKeys(false)
        val teams = mutableListOf<Team>()
        for (teamName in teamNames) {
            val spawns: MutableList<Location>

            // Should never happen, but just in case...
            val teamSection = teamsSection.getConfigurationSection(teamName) ?: continue
            try {
                spawns = teamSection.getLocationList("spawns") as MutableList<Location>
            } catch (e: LocationFormatException) {
                return Err(
                    IllegalWarzoneException(
                        "Error when parsing spawns:\n$e"
                    )
                )
            }
            if (spawns.isEmpty()) {
                return Err(
                    IllegalWarzoneException(
                        "No spawns defined for team $teamName!"
                    )
                )
            }
            spawns.retainAll {
                val contains = region.contains(it)
                if (!contains) {
                    plugin.logger.warning("Spawn for team $teamName is out of bounds!")
                }
                contains
            }

            val team = Team(
                name = teamName,
                spawns = spawns,
                warzone = warzone,
                settings = teamSection.getConfigurationSection("settings") ?: teamSettings
            )
            teams.add(team)
            warzone.addTeam(team)
        }

        return Ok(warzone)
    }

    fun getWarzone(name: String): Warzone? {
        return this.warzones[name.toLowerCase()]
    }
}