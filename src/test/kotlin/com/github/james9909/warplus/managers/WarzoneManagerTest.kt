package com.github.james9909.warplus.managers

import be.seeseemelk.mockbukkit.MockBukkit
import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.config.TeamConfigType
import com.github.james9909.warplus.config.WarzoneConfigType
import com.github.james9909.warplus.objectives.BombObjective
import com.github.james9909.warplus.objectives.CapturePointObjective
import com.github.james9909.warplus.objectives.FlagObjective
import com.github.james9909.warplus.objectives.MonumentObjective
import com.github.james9909.warplus.structures.SpawnStyle
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.potion.PotionEffectType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WarzoneManagerTest {
    private val server = MockBukkit.mock()
    private val plugin = MockBukkit.load(WarPlus::class.java)

    private val warzoneManager = WarzoneManager(plugin)

    init {
        server.addSimpleWorld("flat")
        plugin.config.load(File("src/test/resources/fixtures/config/config.yml"))
    }

    @AfterAll
    internal fun afterAll() {
        MockBukkit.unmock()
    }

    @Test
    fun `loads warzone files correctly`() {
        val configFile = File("src/test/resources/fixtures/config/warzone-valid.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)

        val result = warzoneManager.loadWarzone("valid", config)
        assert(result is Ok)

        val warzone = result.unwrap()
        assert(warzone.name == "valid")
        assert(warzone.isEnabled())
        assert(warzone.teams.size == 2)
        warzone.teams[TeamKind.BLUE]?.apply {
            assert(kind == TeamKind.BLUE)
            assert(spawns.size == 1)
            spawns[0].apply {
                assert(origin.x == 50.0)
                assert(origin.y == 50.0)
                assert(origin.z == 50.0)
                assert(origin.world?.name == "flat")
            }
        } ?: fail("Blue team is null")

        warzone.teams[TeamKind.RED]?.apply {
            assert(kind == TeamKind.RED)
            assert(spawns.size == 1)
            spawns[0].apply {
                assert(origin.x == 25.0)
                assert(origin.y == 25.0)
                assert(origin.z == 25.0)
                assert(origin.world?.name == "flat")
            }
        } ?: fail("Red team is null")
        assert(warzone.minPlayers() == 2)
        assert(warzone.maxPlayers() == 40)

        assert(warzone.objectives.size == 4)
        val bombObjective = warzone.objectives["bombs"] as? BombObjective
        require(bombObjective != null)
        assert(bombObjective.bombs.size == 1)
        bombObjective.bombs[0].apply {
            assert(name == "Bomb1")
            assert(origin.x == 20.0)
            assert(origin.y == 21.0)
            assert(origin.z == 22.0)
        }

        val cpObjective = warzone.objectives["capture_points"] as? CapturePointObjective
        require(cpObjective != null)
        assert(cpObjective.capturePoints.size == 1)
        cpObjective.capturePoints[0].apply {
            assert(name == "Capture Point 1")
            assert(origin.x == 70.0)
            assert(origin.y == 80.0)
            assert(origin.z == 90.0)
        }

        val flagObjective = warzone.objectives["flags"] as? FlagObjective
        require(flagObjective != null)
        assert(flagObjective.flags.size == 2)
        flagObjective.flags[0].apply {
            assert(kind == TeamKind.BLUE)
            assert(origin.x == 60.0)
            assert(origin.y == 50.0)
            assert(origin.z == 40.0)
        }
        flagObjective.flags[1].apply {
            assert(kind == TeamKind.RED)
            assert(origin.x == 40.0)
            assert(origin.y == 50.0)
            assert(origin.z == 60.0)
        }

        val monumentObjective = warzone.objectives["monuments"] as? MonumentObjective
        require(monumentObjective != null)
        assert(monumentObjective.monuments.size == 1)
        monumentObjective.monuments[0].apply {
            assert(name == "Monument1")
            assert(origin.x == 10.0)
            assert(origin.y == 11.0)
            assert(origin.z == 12.0)
            assert(potionEffects.size == 1)
            assert(potionEffects[0].type == PotionEffectType.REGENERATION)
            assert(potionEffects[0].duration == 160)
            assert(potionEffects[0].amplifier == 1)
        }

        val reward = warzone.reward
        assert(reward.winReward.size == 1)
        assert(reward.winReward[0].first.type == Material.DIAMOND)
        assert(reward.winReward[0].first.amount == 5)
        assert(reward.winReward[0].second != null)

        assert(reward.lossReward.size == 1)
        assert(reward.lossReward[0].first.type == Material.DIAMOND)
        assert(reward.lossReward[0].first.amount == 2)
        assert(reward.lossReward[0].second != null)

        assert(reward.mvpReward.size == 1)
        assert(reward.mvpReward[0].first.type == Material.EMERALD)
        assert(reward.mvpReward[0].first.amount == 1)
        assert(reward.mvpReward[0].second != null)
    }

    @Test
    fun `handles cascading settings properly`() {
        val configFile = File("src/test/resources/fixtures/config/warzone-valid.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)

        val result = warzoneManager.loadWarzone("valid", config)
        assert(result is Ok)

        val warzone = result.unwrap()
        assert(warzone.warzoneSettings.get(WarzoneConfigType.ENABLED))
        assert(warzone.warzoneSettings.get(WarzoneConfigType.MAX_HEALTH) == 40.0)

        warzone.teams[TeamKind.RED]?.apply {
            assert(settings.get(TeamConfigType.LIVES) == 5)
            assert(settings.get(TeamConfigType.SPAWN_STYLE) == SpawnStyle.LARGE)
            assert(settings.get(TeamConfigType.MIN_PLAYERS) == 1)
            assert(settings.get(TeamConfigType.MAX_PLAYERS) == 20)
            assert(settings.get(TeamConfigType.MAX_SCORE) == 5)
        } ?: fail("Team red is null")

        warzone.teams[TeamKind.BLUE]?.apply {
            assert(settings.get(TeamConfigType.LIVES) == 30)
            assert(settings.get(TeamConfigType.SPAWN_STYLE) == SpawnStyle.SMALL)
            assert(settings.get(TeamConfigType.MIN_PLAYERS) == 1)
            assert(settings.get(TeamConfigType.MAX_PLAYERS) == 20)
            assert(settings.get(TeamConfigType.MAX_SCORE) == 5)
        } ?: fail("Team blue is null")
    }
}
