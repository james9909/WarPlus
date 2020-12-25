package com.github.james9909.warplus.extensions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ListUtilsTest {
    @Test
    fun `can retrieve all pairs in a list`() {
        val list = listOf("a", "b", "c", "d")
        assert(
            list.pairs().toList()
            ==
            listOf(Pair("a", "b"), Pair("a", "c"), Pair("a", "d"), Pair("b", "c"), Pair("b", "d"), Pair("c", "d"))
        )
        assert(
            listOf<Int>().pairs().toList()
            ==
            listOf<Pair<Int, Int>>()
        )
    }
}
