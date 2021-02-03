package com.github.james9909.warplus.extensions

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

fun uuidFromBytes(bytes: ByteArray): UUID {
    val buf = ByteBuffer.wrap(bytes)
    // `getLong` has side effects, so let's be explicit about that
    return UUID(buf.getLong(), buf.getLong())
}

fun UUID.toBytes(): ByteArray {
    val bytes = ByteArray(16)
    ByteBuffer.wrap(bytes)
        .order(ByteOrder.BIG_ENDIAN)
        .putLong(mostSignificantBits)
        .putLong(leastSignificantBits)
    return bytes
}
