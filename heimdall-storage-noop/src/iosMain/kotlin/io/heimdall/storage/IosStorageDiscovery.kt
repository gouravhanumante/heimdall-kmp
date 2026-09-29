package io.heimdall.storage

import io.heimdall.core.Heimdall

/** Release build stand-in for `heimdall-storage`: discovers nothing. */
fun Heimdall.discoverStorage(appGroupSuiteNames: List<String> = emptyList()) = Unit

fun Heimdall.discoverUserDefaults(appGroupSuiteNames: List<String> = emptyList()) = Unit

fun Heimdall.discoverKeychain() = Unit
