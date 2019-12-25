package com.github.james9909.warplus.managers

import be.seeseemelk.mockbukkit.MockBukkit
import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarPlus
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import org.bukkit.configuration.file.YamlConfiguration
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
    }

    @AfterAll
    internal fun afterAll() {
        MockBukkit.unload()
    }

    @Test
    fun `loads warzone files correctly`() {
        val configFile = File("src/test/resources/fixtures/config/warzone-valid.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)

        val result = warzoneManager.loadWarzone("valid", config)
        assert(result is Ok)

        val warzone = result.unwrap()
        assert(warzone.name == "valid")
        assert(warzone.enabled)
        assert(warzone.teams.size == 2)
        warzone.teams[TeamKind.NAVY]?.apply {
            assert(name == "navy")
            assert(spawns.size == 1)
            spawns[0].apply {
                assert(origin.x == 50.0)
                assert(origin.y == 50.0)
                assert(origin.z == 50.0)
                assert(origin.world?.name == "flat")
            }
        } ?: fail("Navy team is null")

        warzone.teams[TeamKind.RED]?.apply {
            assert(name == "red")
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
    }
}
