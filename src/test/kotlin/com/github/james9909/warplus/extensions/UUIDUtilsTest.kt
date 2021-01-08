package com.github.james9909.warplus.extensions

import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UUIDUtilsTest {

    @Test
    fun `can convert to and from ByteArray`() {
        val uuid = UUID.randomUUID()
        val bytes = uuid.toBytes()
        assert(uuidFromBytes(bytes) == uuid)
    }
}
