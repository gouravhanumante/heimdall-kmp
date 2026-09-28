package io.heimdall.sample.shared

import androidx.compose.ui.window.ComposeUIViewController
import io.heimdall.ui.HeimdallOverlayController
import platform.UIKit.UIViewController

/**
 * Hosts [SampleApp] for Xcode — see sample/iosApp. Shake capture is not wired in yet: iOS
 * shake/overlay-window work is deferred together, see docs/TODO.md.
 */
fun MainViewController(): UIViewController {
    val overlayController = HeimdallOverlayController()
    return ComposeUIViewController { SampleApp(overlayController) }
}
