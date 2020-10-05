package com.github.james9909.warplus.util

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.entity.PlayerMock
import com.github.james9909.warplus.WarPlus
import org.bukkit.Location
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlayerStateTest {
    private val server = MockBukkit.mock()
    private val plugin = MockBukkit.load(WarPlus::class.java)
    private lateinit var player: PlayerMock

    init {
        server.addSimpleWorld("flat")
    }

    @BeforeEach
    internal fun beforeEach() {
        player = PlayerMock(server, "test", UUID.randomUUID())
    }

    @AfterAll
    internal fun afterAll() {
        MockBukkit.unmock()
    }

    @Test
    @Disabled("not ready yet")
    fun `saves and restores player state`() {
        val location = Location(null, 1.0, 2.0, 3.0, 4F, 5F)
        player.teleport(location)

        val state = PlayerState.fromPlayer(player)
        player.teleport(location.zero())

        state.restore(player)
        assert(player.location == location)
    }
}
