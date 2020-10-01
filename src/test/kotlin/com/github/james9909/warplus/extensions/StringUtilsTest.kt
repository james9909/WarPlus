package com.github.james9909.warplus.extensions

import be.seeseemelk.mockbukkit.MockBukkit
import com.github.james9909.warplus.WarPlus
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StringUtilsTest {
    private val server = MockBukkit.mock()
    private val plugin = MockBukkit.load(WarPlus::class.java)

    init {
        server.addSimpleWorld("flat")
    }

    @AfterAll
    internal fun afterAll() {
        MockBukkit.unmock()
    }

    @Nested
    inner class GetLocation {
        @Test
        fun `parses 3 coordinates as a location`() {
            val location = "flat:1,2,3".toLocation()
            assert(location.x == 1.0)
            assert(location.blockX == 1)
            assert(location.y == 2.0)
            assert(location.blockY == 2)
            assert(location.z == 3.0)
            assert(location.blockZ == 3)

            // Default values
            assert(location.yaw == 0F)
            assert(location.pitch == 0F)
        }

        @Test
        fun `parses 5 coordinates as a location`() {
            val location = "flat:1,2,3,4,5".toLocation()
            assert(location.x == 1.0)
            assert(location.blockX == 1)
            assert(location.y == 2.0)
            assert(location.blockY == 2)
            assert(location.z == 3.0)
            assert(location.blockZ == 3)
            assert(location.yaw == 4F)
            assert(location.pitch == 5F)
        }

        @Test
        fun `fails to parse an invalid world`() {
            assertThrows<LocationFormatException> {
                "world:1,2,3".toLocation()
            }
        }

        @Test
        fun `fails when only given coordinates`() {
            assertThrows<LocationFormatException> {
                "1,2,3".toLocation()
            }
        }

        @Test
        fun `fails when only given a world`() {
            assertThrows<LocationFormatException> {
                "flat".toLocation()
            }
        }

        @Test
        fun `fails when coordinates are invalid`() {
            assertThrows<LocationFormatException> {
                "flat:a,b,c".toLocation()
            }
        }
    }
}
