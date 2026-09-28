package io.heimdall.sample.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.heimdall.ui.HeimdallOverlay
import io.heimdall.ui.HeimdallOverlayController

/**
 * Shared sample screen used by both sample/androidApp and sample/iosApp so the two platforms
 * exercise the exact same overlay wiring — divergence here would defeat the point of a shared
 * sample. Platform entry points only differ in how they host this and how they feed a shake.
 */
@Composable
fun SampleApp(overlayController: HeimdallOverlayController) {
    MaterialTheme {
        HeimdallOverlay(controller = overlayController) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Heimdall sample app")
            }
        }
    }
}
