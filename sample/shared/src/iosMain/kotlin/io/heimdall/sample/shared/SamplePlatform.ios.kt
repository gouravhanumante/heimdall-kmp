package io.heimdall.sample.shared

import androidx.compose.runtime.Composable

@Composable
actual fun SampleBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit

actual fun samplePlatformLog(tag: String, message: String) {
    println("$tag: $message")
}