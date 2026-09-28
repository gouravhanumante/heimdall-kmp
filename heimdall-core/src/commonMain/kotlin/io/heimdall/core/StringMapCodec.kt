package io.heimdall.core

/** Tiny length-prefixed encoding for `Map<String, String>` so header/tag maps can be stored as a
 * single TEXT column without a JSON dependency or escaping rules — each key/value is stored as
 * its byte length (in UTF-16 chars) followed by the raw chars, so no character in the value
 * (including newlines or the delimiter itself) needs escaping. */
internal fun encodeStringMap(map: Map<String, String>): String = buildString {
    for ((key, value) in map) {
        append(key.length).append(':').append(key)
        append(value.length).append(':').append(value)
    }
}

internal fun decodeStringMap(encoded: String): Map<String, String> {
    if (encoded.isEmpty()) return emptyMap()
    val result = mutableMapOf<String, String>()
    var index = 0
    while (index < encoded.length) {
        val (key, afterKey) = readLengthPrefixed(encoded, index)
        val (value, afterValue) = readLengthPrefixed(encoded, afterKey)
        result[key] = value
        index = afterValue
    }
    return result
}

private fun readLengthPrefixed(source: String, start: Int): Pair<String, Int> {
    val colonIndex = source.indexOf(':', start)
    val length = source.substring(start, colonIndex).toInt()
    val valueStart = colonIndex + 1
    val valueEnd = valueStart + length
    return source.substring(valueStart, valueEnd) to valueEnd
}
