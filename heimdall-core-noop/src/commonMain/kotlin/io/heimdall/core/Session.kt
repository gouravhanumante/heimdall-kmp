package io.heimdall.core

data class Session(
    val id: Long,
    val startedAtMillis: Long,
    val crashed: Boolean,
)
