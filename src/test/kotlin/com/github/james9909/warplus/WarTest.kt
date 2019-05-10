package com.github.james9909.warplus

import be.seeseemelk.mockbukkit.MockBukkit
import junit.framework.TestCase.assertNotNull
import org.bukkit.event.server.PluginEnableEvent
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

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
        assertNotNull(server.pluginManager.getPlugin("warplus"))
        assert(plugin.isEnabled)
    }
}