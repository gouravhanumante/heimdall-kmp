package io.heimdall.network.ktor

import io.heimdall.core.Heimdall
import io.heimdall.core.NetworkRecord
import io.ktor.client.call.save
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentType
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random

/**
 * Install on an existing `HttpClient` to record every call into `Heimdall.network` — this plugin
 * never creates or owns the client.
 *
 * Captures via the `Send` phase and [io.ktor.client.call.HttpClientCall.save], not
 * `onResponse`/`transformResponseBody`: those only fire for typed `.body<T>()` reads through
 * content negotiation, so a caller using `bodyAsText()`/`bodyAsChannel()` directly — a very
 * common case — would otherwise never be captured. `save()` buffers the response once and
 * returns a replayable call, so the app downstream still sees a fresh, fully-readable response
 * either way.
 */
val HeimdallKtor = createClientPlugin("HeimdallKtor", ::HeimdallKtorConfig) {
    val redactedHeaders = pluginConfig.redactedHeaders
    val maxCapturedBodyBytes = pluginConfig.maxCapturedBodyBytes

    on(Send) { request ->
        if (!Heimdall.enabled) return@on proceed(request)

        val callId = randomCallId()
        val startedAt = epochMillisNow()
        val requestHeaders = redact(headerMap(request.headers.entries()), redactedHeaders)
        val requestBody = bodyPreview(request)

        val result = runCatching { proceed(request).save() }
        val durationMillis = epochMillisNow() - startedAt

        result.fold(
            onSuccess = { call ->
                Heimdall.network.record(
                    buildRecord(
                        callId, request, requestHeaders, requestBody, call.response,
                        startedAt, durationMillis, maxCapturedBodyBytes, error = null,
                    ),
                )
                call
            },
            onFailure = { throwable ->
                if (throwable is CancellationException) throw throwable
                Heimdall.network.record(
                    NetworkRecord(
                        id = callId,
                        method = request.method.value,
                        url = request.url.buildString(),
                        requestHeaders = requestHeaders,
                        requestBody = requestBody,
                        statusCode = null,
                        responseHeaders = emptyMap(),
                        responseBody = null,
                        startedAtMillis = startedAt,
                        durationMillis = durationMillis,
                        error = throwable.message ?: throwable::class.simpleName,
                    ),
                )
                throw throwable
            },
        )
    }
}

private suspend fun buildRecord(
    callId: String,
    request: HttpRequestBuilder,
    requestHeaders: Map<String, String>,
    requestBody: String?,
    response: HttpResponse,
    startedAt: Long,
    durationMillis: Long,
    maxCapturedBodyBytes: Long,
    error: String?,
): NetworkRecord {
    val responseHeaders = response.headers.entries().associate { it.key to it.value.joinToString("; ") }
    val responseBody = if (isTextish(response)) {
        runCatching { response.bodyAsText() }.getOrNull()?.let { text ->
            if (text.length.toLong() <= maxCapturedBodyBytes) {
                text
            } else {
                "«${text.length} chars, exceeds $maxCapturedBodyBytes-char capture cap»"
            }
        }
    } else {
        null
    }

    return NetworkRecord(
        id = callId,
        method = request.method.value,
        url = request.url.buildString(),
        requestHeaders = requestHeaders,
        requestBody = requestBody,
        statusCode = response.status.value,
        responseHeaders = responseHeaders,
        responseBody = responseBody,
        startedAtMillis = startedAt,
        durationMillis = durationMillis,
        error = error,
    )
}

private fun isTextish(response: HttpResponse): Boolean {
    val contentType = response.contentType() ?: return false
    return contentType.contentType == "text" ||
        contentType.contentSubtype.contains("json") ||
        contentType.contentSubtype.contains("xml")
}

private fun bodyPreview(request: HttpRequestBuilder): String? = when (val content = request.body) {
    is io.ktor.client.utils.EmptyContent -> null
    is ByteArray -> "«binary, ${content.size} bytes»"
    else -> content.toString().takeIf { it.isNotBlank() }
}

private fun headerMap(entries: Set<Map.Entry<String, List<String>>>): Map<String, String> =
    entries.associate { it.key to it.value.joinToString("; ") }

private fun randomCallId(): String = Random.nextLong().toString(radix = 36)

internal expect fun epochMillisNow(): Long
