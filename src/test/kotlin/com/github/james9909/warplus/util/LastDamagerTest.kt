package com.github.james9909.warplus.util

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.entity.PlayerMock
import com.github.james9909.warplus.WarPlus
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LastDamagerTest {
    private val server = MockBukkit.mock()
    private val plugin = MockBukkit.load(WarPlus::class.java)

    init {
        server.addSimpleWorld("flat")
    }

    @AfterAll
    internal fun afterAll() {
        MockBukkit.unmock()
    }

    @Test
    fun `can update damager`() {
        val player1 = PlayerMock(server, "test1", UUID.randomUUID())
        val player2 = PlayerMock(server, "test2", UUID.randomUUID())

        val lastDamager = LastDamager(player1)
        assert(lastDamager.damager == player1)
        lastDamager.damager = player2
        assert(lastDamager.damager == player2)
    }

    @Test
    fun `expires damagers appropriately`() {
        val player = PlayerMock(server, "test", UUID.randomUUID())
        val lastDamager = LastDamager(player)
        assert(lastDamager.damager == player)
        TimeUnit.SECONDS.sleep(6)
        assert(lastDamager.damager == null)
    }
}
