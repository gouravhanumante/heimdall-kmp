package io.heimdall.core

/**
 * What [Heimdall.install] needs from the host platform to find a writable, app-private
 * directory. On Android this is a real `Context`; iOS has no equivalent so it's an empty marker
 * — [resolveDatabasePath] finds the app's own sandbox without one.
 */
expect class PlatformContext

/** Absolute path to [fileName] inside a directory only this app can read — never the app's own
 * data directory, so a consumer's backup/restore or file-scan of their own data never touches
 * Heimdall's database by accident. */
expect fun resolveDatabasePath(context: PlatformContext, fileName: String): String
