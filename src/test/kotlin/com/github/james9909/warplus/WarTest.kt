package com.github.james9909.warplus

import be.seeseemelk.mockbukkit.MockBukkit
import org.bukkit.event.server.PluginEnableEvent
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WarTest {
    private val server = MockBukkit.mock()
    private val plugin = MockBukkit.load(WarPlus::class.java)

    init {
        server.addSimpleWorld("flat")
    }

    @AfterAll
    internal fun afterAll() {
        MockBukkit.unload()
    }

    @Test
    fun `plugin loads correctly`() {
        server.pluginManager.assertEventFired(PluginEnableEvent::class.java) { event: PluginEnableEvent -> event.plugin == plugin }
        requireNotNull(server.pluginManager.getPlugin("WarPlus"))
        assert(plugin.isEnabled)
    }

    @Test
    fun `plugin saves default config if necessary`() {
        val actualConfigFile = File(plugin.dataFolder, "config.yml")
        val testConfigFile = File("src/main/resources/config.yml")
        assert(actualConfigFile.exists())
        assert(actualConfigFile.readText().trim() == testConfigFile.readText().trim())
    }
}
