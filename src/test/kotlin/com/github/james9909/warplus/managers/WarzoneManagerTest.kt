package com.github.james9909.warplus.managers

import be.seeseemelk.mockbukkit.MockBukkit
import com.github.james9909.warplus.Ok
import com.github.james9909.warplus.WarPlus
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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

        val warzone = result.get()
        assert(warzone.name == "valid")
        assert(warzone.enabled)
        assert(warzone.teams.size == 2)
        warzone.teams.apply {
            this[0].apply {
                assert(name == "navy")
                assert(spawns.size == 1)
                spawns[0].apply {
                    assert(origin.x == 50.0)
                    assert(origin.y == 50.0)
                    assert(origin.z == 50.0)
                    assert(origin.world?.name == "flat")
                }
            }
            this[1].apply {
                assert(name == "red")
                assert(spawns.size == 1)
                spawns[0].apply {
                    assert(origin.x == 25.0)
                    assert(origin.y == 25.0)
                    assert(origin.z == 25.0)
                    assert(origin.world?.name == "flat")
                }
            }
        }
        assert(warzone.minPlayers() == 2)
        assert(warzone.maxPlayers() == 40)
    }
}
