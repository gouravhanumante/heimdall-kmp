package io.heimdall.core

/** One app run, from process start to process death — not from foreground/background, since
 * backgrounding for hours and resuming is still the same run for debugging purposes. See
 * docs/architecture.md. Only history (network, logs, crashes) is tagged with a session; live state
 * (storage, app databases, flags) is not, so the panel can show one run's history at a time. */
data class Session(
    val id: Long,
    val startedAtMillis: Long,
    val crashed: Boolean,
)
