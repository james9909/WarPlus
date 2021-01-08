package com.github.james9909.warplus.managers

import be.seeseemelk.mockbukkit.MockBukkit
import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.sql.SqliteConnectionFactory
import com.github.michaelbull.result.Ok
import java.io.File
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseManagerTest {
    private val server = MockBukkit.mock()
    private val plugin = MockBukkit.load(WarPlus::class.java)

    private val databaseManager = DatabaseManager(
        plugin,
        SqliteConnectionFactory(
            "test.db"
        )
    )

    @BeforeEach
    internal fun beforeEach() {
        assert(databaseManager.createTables() is Ok)
    }

    @AfterEach
    internal fun afterEach() {
        assert(databaseManager.dropTables() is Ok)
    }

    @BeforeAll
    internal fun beforeAll() {
        File("test.db").delete()
    }

    @AfterAll
    internal fun afterAll() {
        MockBukkit.unmock()
    }

    @Test
    fun `creates and drops tables`() { }

    @Test
    fun `adds warzones with incrementing IDs`() {
        assert(databaseManager.addWarzone("testzone") == 1)
        assert(databaseManager.addWarzone("testzone") == 2)
    }

    @Test
    fun `gets a warzone by id`() {
        assert(databaseManager.addWarzone("testzone") == 1)
        val warzone = databaseManager.getWarzone(1)
        assert(warzone != null)
        require(warzone != null)
        assert(warzone.id == 1)
        assert(warzone.winners == null)
        assert(warzone.endTime == null)
    }

    @Test
    fun `ends a warzone with a winner and end time`() {
        assert(databaseManager.addWarzone("testzone") == 1)
        var warzone = databaseManager.getWarzone(1)
        assert(warzone != null)
        require(warzone != null)

        assert(warzone.id == 1)
        assert(warzone.name == "testzone")
        assert(warzone.endTime == null)
        assert(warzone.winners == null)

        databaseManager.endWarzone(1, listOf(TeamKind.BLUE))
        warzone = databaseManager.getWarzone(1)
        assert(warzone != null)
        require(warzone != null)

        assert(warzone.id == 1)
        assert(warzone.name == "testzone")
        assert(warzone.endTime != null)
        assert(warzone.winners == listOf(TeamKind.BLUE))
    }
}
