package io.heimdall.network.ktor

import io.ktor.client.plugins.api.createClientPlugin

/**
 * Release build stand-in for `heimdall-network-ktor`: installs on the client but captures
 * nothing, and never reads a request/response body it wouldn't otherwise. See
 * docs/release-builds.md.
 */
val HeimdallKtor = createClientPlugin("HeimdallKtor", ::HeimdallKtorConfig) {}
