package io.heimdall.core

/** A capped, newest-first buffer. Older entries fall off once [capacity] is exceeded, so a long
 * debug session can't grow this without bound. Not thread-safe by itself — callers synchronize. */
internal class RingBuffer<T>(private val capacity: Int) {
    private val items = ArrayDeque<T>(capacity)

    fun push(item: T) {
        items.addFirst(item)
        while (items.size > capacity) items.removeLast()
    }

    fun snapshot(): List<T> = items.toList()

    fun clear() = items.clear()
}
