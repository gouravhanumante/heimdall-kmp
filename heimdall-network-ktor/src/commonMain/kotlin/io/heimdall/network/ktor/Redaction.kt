package io.heimdall.network.ktor

/** Header names masked wherever a request/response is recorded — these tend to be the ones
 * that end up pasted into a bug report or a chat if left readable. Override via
 * [HeimdallKtorConfig.redactedHeaders]; pass an empty set to record everything verbatim. */
val defaultRedactedHeaders = setOf(
    "Authorization",
    "Cookie",
    "Set-Cookie",
    "Proxy-Authorization",
    "X-Api-Key",
)

const val REDACTED_VALUE = "«redacted»"

fun redact(headers: Map<String, String>, redactedKeys: Set<String>): Map<String, String> {
    if (redactedKeys.isEmpty()) return headers
    val redactedKeysLower = redactedKeys.map { it.lowercase() }.toSet()
    return headers.mapValues { (key, value) ->
        if (key.lowercase() in redactedKeysLower) REDACTED_VALUE else value
    }
}
