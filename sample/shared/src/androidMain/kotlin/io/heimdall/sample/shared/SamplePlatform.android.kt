package io.heimdall.sample.shared

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun SampleBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

actual fun samplePlatformLog(tag: String, message: String) {
    Log.d(tag, message)
}