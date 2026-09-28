package io.heimdall.core

/**
 * Single entry point every collector reports into and the panel reads from. `enabled` is the one
 * runtime kill switch that exists today — every store's `record`/`publish` call checks it. It
 * does **not** remove any code from a release binary; see docs/release-builds.md for what that
 * would take and why this alone isn't it.
 */
object Heimdall {
    var enabled: Boolean = true

    val network = NetworkStore()
    val logs = LogStore()
    val crashes = CrashStore()
    val storage = StorageStore()
    val database = DatabaseStore()
    val flags = FlagStore()
}
