package io.heimdall.core

/** No automatic hooks on iOS yet — frame/jank monitoring was removed (it measured Heimdall's own
 * popup along with the app, with no way to tell them apart), and there is no iOS equivalent of
 * Android's uncaught-exception handler wired here yet; see docs/TODO.md. */
internal actual fun installPlatformHooks(context: PlatformContext) {
}