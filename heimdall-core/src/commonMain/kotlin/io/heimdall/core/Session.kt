package io.heimdall.core

/** One app run, from process start to process death — not from foreground/background, since
 * backgrounding for hours and resuming is still the same run for debugging purposes. See
 * docs/plugins/sessions.md for the reasoning. Everything else (network, logs, crashes) is
 * tagged with the session it happened in, so the panel can show one run at a time. */
data class Session(
    val id: Long,
    val startedAtMillis: Long,
    val crashed: Boolean,
)
