package io.heimdall.network.ktor

class HeimdallKtorConfig {
    /** Header names masked in captured requests/responses. Empty (the default) records
     * everything verbatim, including auth tokens, so a captured call can be copy-pasted as a
     * working `curl` command — this is a local, on-device debug capture, not something shared
     * automatically. Pass `commonSensitiveHeaders` (or your own set) to opt into masking. */
    var redactedHeaders: Set<String> = emptySet()

    /** Response bodies are only buffered for capture when the response declares a text-ish
     * content type (text/json/xml/…); anything else (image, video, octet-stream, …) is recorded
     * as headers/status only. The full body is always read and passed through unchanged either
     * way — this cap only limits how much of a *text-ish* body is kept as a readable preview; a
     * body larger than this is recorded as a size note instead of its content. */
    var maxCapturedBodyBytes: Long = 1_000_000L
}
