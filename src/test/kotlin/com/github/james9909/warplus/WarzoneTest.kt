package com.github.james9909.warplus

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.entity.PlayerMock
import com.github.michaelbull.result.unwrap
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WarzoneTest {
    private val server = MockBukkit.mock()
    private val plugin = MockBukkit.load(WarPlus::class.java)
    private val classConfig = File("src/test/resources/fixtures/config/classes.yml")
    private val configFile = File("src/test/resources/fixtures/config/warzone-valid.yml")
    private val warzone: Warzone

    init {
        server.addSimpleWorld("flat")

        val classesConfig = YamlConfiguration.loadConfiguration(classConfig)
        plugin.classManager.loadClasses(classesConfig)
        val config = YamlConfiguration.loadConfiguration(configFile)
        warzone = plugin.warzoneManager.loadWarzone("valid", config).unwrap()
    }

    @AfterAll
    internal fun afterAll() {
        MockBukkit.unload()
    }

    @Test
    @Disabled("not ready yet")
    fun `joining correctly assigns a player to a team`() {
        val player = PlayerMock(server, "test", UUID.randomUUID())
        warzone.addPlayer(player)
        val playerInfo = plugin.playerManager.getPlayerInfo(player)
        assert(playerInfo != null)
        assert(playerInfo!!.team.warzone == warzone)
    }

    @Test
    fun `saving warzones works`() {
        warzone.saveConfig()
        val actualConfigFile = File("${plugin.dataFolder}/warzone-valid.yml")
        assert(actualConfigFile.exists())
        assert(actualConfigFile.readText().trim() == configFile.readText().trim())
    }
}
