package io.heimdall.sample.shared

import androidx.compose.runtime.Composable

@Composable
expect fun SampleBackHandler(enabled: Boolean, onBack: () -> Unit)

expect fun samplePlatformLog(tag: String, message: String)