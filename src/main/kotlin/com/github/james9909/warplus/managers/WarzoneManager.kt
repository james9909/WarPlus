package com.github.james9909.warplus.managers

import com.github.james9909.warplus.DEFAULT_TEAM_CONFIG
import com.github.james9909.warplus.DEFAULT_WARZONE_CONFIG
import com.github.james9909.warplus.IllegalTeamKindError
import com.github.james9909.warplus.IllegalWarzoneError
import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarError
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.WarTeam
import com.github.james9909.warplus.Warzone
import com.github.james9909.warplus.config.CascadingConfig
import com.github.james9909.warplus.config.TeamConfigType
import com.github.james9909.warplus.extensions.LocationFormatException
import com.github.james9909.warplus.extensions.getLocationFromString
import com.github.james9909.warplus.extensions.getOrCreateSection
import com.github.james9909.warplus.extensions.toLocation
import com.github.james9909.warplus.objectives.createCapturePointObjective
import com.github.james9909.warplus.objectives.createFlagObjective
import com.github.james9909.warplus.objectives.createMonumentObjective
import com.github.james9909.warplus.region.Region
import com.github.james9909.warplus.structures.TeamSpawnStructure
import com.github.james9909.warplus.structures.WarzonePortalStructure
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration

class WarzoneManager(val plugin: WarPlus) {
    private val warzones = mutableMapOf<String, Warzone>()

    fun loadWarzones() {
        warzones.clear()
        plugin.dataFolder.listFiles()?.forEach {
            if (!it.name.startsWith("warzone-") || it.extension != "yml") {
                return@forEach
            }
            val name = it.nameWithoutExtension.substring(8)
            plugin.logger.info("Loading zone $name")
            when (val result = loadWarzone(name, YamlConfiguration.loadConfiguration(it))) {
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
        val objectivesSection = config.getOrCreateSection("objectives")
        val portalsSection = config.getOrCreateSection("portals")

        // Get region information
        val worldName = infoSection.getString("world") ?: plugin.server.worlds[0].name
        val world = plugin.server.getWorld(worldName) ?: plugin.server.worlds[0]

        val p1: Location
        val p2: Location
        try {
            p1 = infoSection.getLocationFromString("p1") ?: return Err(
                IllegalWarzoneError(
                    "No p1 defined"
                )
            )
            p2 = infoSection.getLocationFromString("p2") ?: return Err(
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
        val teamDefaultConfig = plugin.config.getConfigurationSection("team.default.config") ?: YamlConfiguration()
        val warzoneDefaultConfig =
            plugin.config.getConfigurationSection("warzone.default.config") ?: YamlConfiguration()
        val warzone = Warzone(
            plugin = plugin,
            name = name,
            region = region,
            teamSettings = CascadingConfig(
                teamSettings, CascadingConfig(
                    teamDefaultConfig, CascadingConfig(
                        DEFAULT_TEAM_CONFIG
                    )
                )
            ),
            warzoneSettings = CascadingConfig(
                zoneSettings, CascadingConfig(
                    warzoneDefaultConfig, CascadingConfig(
                        DEFAULT_WARZONE_CONFIG
                    )
                )
            ),
            classes = config.getStringList("classes")
        )
        warzone.restoreVolume()

        // Get teams
        val teamNames = teamsSection.getKeys(false)
        val teams = mutableListOf<WarTeam>()
        for (teamName in teamNames) {
            val spawns: MutableList<TeamSpawnStructure> = mutableListOf()

            val teamSection = teamsSection.getConfigurationSection(teamName) ?: continue
            val overloadedTeamSettings = teamSection.getConfigurationSection("settings") ?: YamlConfiguration()
            val cascadingTeamSettings = CascadingConfig(overloadedTeamSettings, warzone.teamSettings)
            try {
                for (spawnLocation in teamSection.getStringList("spawns")) {
                    val spawn =
                        TeamSpawnStructure(
                            plugin,
                            spawnLocation.toLocation(),
                            TeamKind.valueOf(teamName.toUpperCase()),
                            cascadingTeamSettings.get(TeamConfigType.SPAWN_STYLE)
                        )
                    spawns.add(spawn)
                }
            } catch (e: LocationFormatException) {
                return Err(
                    IllegalWarzoneError(
                        "Error when parsing spawns:\n$e"
                    )
                )
            } catch (e: IllegalArgumentException) {
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

            val teamKind: TeamKind
            try {
                teamKind = TeamKind.valueOf(teamName.toUpperCase())
            } catch (e: IllegalArgumentException) {
                return Err(
                    IllegalTeamKindError(teamName)
                )
            }
            val classes = teamSection.getStringList("classes").filter {
                plugin.classManager.containsClass(it)
            }
            val team = WarTeam(
                kind = teamKind,
                spawns = spawns,
                warzone = warzone,
                settings = cascadingTeamSettings,
                classes = classes
            )
            if (plugin.hasPlugin("WorldEdit")) {
                team.resetSpawns()
            }
            teams.add(team)
            warzone.addTeam(team)
        }

        // Load objectives
        for (objectiveName in objectivesSection.getKeys(false)) {
            val objectiveSection = objectivesSection.getConfigurationSection(objectiveName) ?: continue
            val objective = when (objectiveName) {
                "flags" -> {
                    createFlagObjective(plugin, warzone, objectiveSection)
                }
                "monuments" -> {
                    createMonumentObjective(plugin, warzone, objectiveSection)
                }
                "capture_points" -> {
                    createCapturePointObjective(plugin, warzone, objectiveSection)
                }
                else -> null
            }
            if (objective != null) {
                if (plugin.hasPlugin("WorldEdit")) {
                    objective.reset()
                }
                warzone.objectives[objective.name] = objective
            } else {
                plugin.logger.warning("Could not parse objective: $objectiveName")
            }
        }

        // Load portals
        for (portalName in portalsSection.getKeys(false)) {
            val portalSection = portalsSection.getConfigurationSection(portalName) ?: continue
            val origin = portalSection.getLocationFromString("origin")
            if (origin == null) {
                plugin.logger.warning("Invalid location for portal $portalName")
                continue
            }
            val portal = WarzonePortalStructure(
                plugin, origin, portalName, warzone
            )
            if (plugin.hasPlugin("WorldEdit")) {
                portal.build()
            }
            warzone.addPortal(portal)
        }
        return Ok(warzone)
    }

    fun getWarzoneNames(): List<String> {
        return this.warzones.keys.toList()
    }

    fun getWarzones(): List<Warzone> {
        return this.warzones.values.toList()
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

    fun deleteWarzone(warzone: Warzone): Boolean {
        this.warzones.remove(warzone.name.toLowerCase())
        return warzone.delete()
    }
}
