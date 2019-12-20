package com.github.james9909.warplus.managers

import com.github.james9909.warplus.IllegalWarzoneError
import com.github.james9909.warplus.Team
import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarError
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.extensions.LocationFormatException
import com.github.james9909.warplus.extensions.getLocation
import com.github.james9909.warplus.extensions.getLocationList
import com.github.james9909.warplus.extensions.getOrCreateSection
import com.github.james9909.warplus.extensions.toLocation
import com.github.james9909.warplus.region.Region
import com.github.james9909.warplus.structure.SpawnStyle
import com.github.james9909.warplus.structure.TeamSpawnStructure
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration

class WarzoneManager(val plugin: WarPlus) {
    private val warzones = mutableMapOf<String, Warzone>()

    fun loadWarzones() {
        plugin.dataFolder.listFiles()?.forEach {
            if (!it.name.startsWith("warzone-") || it.extension != "yml") {
                return@forEach
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
    ): Result<Warzone, WarError> {
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
                IllegalWarzoneError(
                    "No p1 defined"
                )
            )
            p2 = infoSection.getLocation("p2") ?: return Err(
                IllegalWarzoneError(
                    "No p2 defined"
                )
            )
        } catch (e: LocationFormatException) {
            return Err(
                IllegalWarzoneError(
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
            val spawns: MutableList<TeamSpawnStructure> = mutableListOf()
            val flags: MutableList<Location>

            // Should never happen, but just in case...
            val teamSection = teamsSection.getConfigurationSection(teamName) ?: continue
            try {
                for (spawnLocation in teamSection.getStringList("spawns")) {
                    spawns.add(
                        TeamSpawnStructure(
                            plugin,
                            spawnLocation.toLocation(),
                            TeamKind.valueOf(teamName.toUpperCase()),
                            SpawnStyle.valueOf(
                                (teamSection.getString("spawnstyle") ?: teamSettings.getString("spawnstyle")
                                ?: "SMALL").toUpperCase()
                            )
                        )
                    )
                }
            } catch (e: LocationFormatException) {
                return Err(
                    IllegalWarzoneError(
                        "Error when parsing spawns:\n$e"
                    )
                )
            }
            spawns.retainAll {
                val contains = region.contains(it.origin)
                if (!contains) {
                    plugin.logger.warning("Spawn for team $teamName is out of bounds!")
                }
                contains
            }
            try {
                flags = teamSection.getLocationList("flags") as MutableList<Location>
            } catch (e: LocationFormatException) {
                return Err(
                    IllegalWarzoneError(
                        "Error when parsing flags:\n$e"
                    )
                )
            }

            val team = Team(
                name = teamName,
                spawns = spawns,
                warzone = warzone,
                flags = flags,
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

    fun addWarzone(warzone: Warzone) {
        this.warzones[warzone.name.toLowerCase()] = warzone
    }

    fun getWarzoneByLocation(location: Location): Warzone? {
        return warzones.values.firstOrNull {
            it.contains(location)
        }
    }

    fun unloadWarzones() {
        for (warzone in warzones) {
            warzone.value.unload()
        }
        this.warzones.clear()
    }
}