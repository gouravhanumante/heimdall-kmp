package io.heimdall.network.ktor

/** Not applied by default (see [HeimdallKtorConfig.redactedHeaders]) — Heimdall's capture is a
 * local debug store meant to be read and copy-pasted as a working `curl` command, so a header
 * redacted here would need to be typed back in by hand. Offered as a named opt-in for apps that
 * want it: `redactedHeaders = commonSensitiveHeaders`. */
val commonSensitiveHeaders = setOf(
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
