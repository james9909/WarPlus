package com.github.james9909.warplus.managers

import be.seeseemelk.mockbukkit.MockBukkit
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.sql.SqliteConnectionFactory
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.AfterAll
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

    @AfterAll
    internal fun afterAll() {
        MockBukkit.unload()
    }

    @Test
    fun `creates and drops tables`() {
        assert(databaseManager.createTables() is Ok)
        assert(databaseManager.dropTables() is Ok)
    }
}
