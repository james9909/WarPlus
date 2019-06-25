package com.github.james9909.warplus

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.entity.PlayerMock
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.Ignore
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WarzoneTest {
    private val server = MockBukkit.mock()
    private val plugin = MockBukkit.load(WarPlus::class.java)
    private val warzone: Warzone

    init {
        server.addSimpleWorld("flat")

        val configFile = File("src/test/resources/fixtures/config/warzone-valid.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        warzone = plugin.warzoneManager.loadWarzone("valid", config).get()
    }

    @AfterAll
    internal fun afterAll() {
        MockBukkit.unload()
    }

    @Test
    @Ignore("not ready yet")
    fun `joining correctly assigns a player to a team`() {
        val player = PlayerMock(server, "test", UUID.randomUUID())
        warzone.addPlayer(player)
        val playerInfo = plugin.playerManager.getPlayerInfo(player)!!
        assert(playerInfo.team != null)
        assert(playerInfo.team?.warzone == warzone)
    }
}
