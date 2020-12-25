package com.github.james9909.warplus.extensions

fun <T> List<T>.pairs(): Sequence<Pair<T, T>> = sequence {
    for (i in 0 until size - 1) {
        for (j in i + 1 until size) {
            yield(get(i) to get(j))
        }
    }
}
