package io.heimdall.network.ktor

class HeimdallKtorConfig {
    var redactedHeaders: Set<String> = emptySet()
    var maxCapturedBodyBytes: Long = 1_000_000L
}
