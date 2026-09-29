package io.heimdall.sample.shared

import io.heimdall.core.Heimdall
import io.heimdall.core.IosShakeListener
import io.heimdall.core.PlatformContext
import io.heimdall.ui.HeimdallOverlayController
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIEvent
import platform.UIKit.UIEventSubtype
import platform.UIKit.UIEventSubtypeMotionShake
import platform.UIKit.UIView
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowLevelAlert
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewController
import platform.UIKit.UIScreen
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRect

@OptIn(ExperimentalForeignApi::class)
private class PassthroughOverlayWindow(
    frame: CValue<CGRect>,
    private val controller: HeimdallOverlayController,
) : UIWindow(frame) {
    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        val (x, y) = point.useContents { this.x to this.y }
        if (!controller.acceptsOverlayTouch(x.toFloat(), y.toFloat())) return null
        return super.hitTest(point, withEvent)
    }
}

private var overlayWindow: PassthroughOverlayWindow? = null

private class ShakeHostViewController(
    private val content: UIViewController,
    private val shakeListener: IosShakeListener,
) : UIViewController(nibName = null, bundle = null) {
    @OptIn(ExperimentalForeignApi::class)
    override fun viewDidLoad() {
        super.viewDidLoad()
        addChildViewController(content)
        content.view.setFrame(view.bounds)
        content.view.autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        view.addSubview(content.view)
        content.didMoveToParentViewController(this)
    }

    override fun motionEnded(motion: UIEventSubtype, withEvent: UIEvent?) {
        if (motion == UIEventSubtypeMotionShake) {
            shakeListener.notifyShakeDetected()
        }
        super.motionEnded(motion, withEvent)
    }
}


/** Hosts [SampleApp] with the shared Compose overlay; shake events are forwarded to the controller. */
fun MainViewController(): UIViewController = createMainViewController(useNativeOverlayWindow = false)

/** Opt-in: the bubble lives in a separate UIWindow so it stays above native screens and dialogs. Unverified on device. */
fun MainViewControllerWithNativeOverlayWindow(): UIViewController =
    createMainViewController(useNativeOverlayWindow = true)

@OptIn(ExperimentalForeignApi::class)
private fun createMainViewController(useNativeOverlayWindow: Boolean): UIViewController {
    Heimdall.install(PlatformContext())
    val overlayController = HeimdallOverlayController()
    val shakeListener = IosShakeListener { overlayController.recall() }
    shakeListener.start()
    val content = ComposeUIViewController {
        SampleApp(overlayController, showOverlay = !useNativeOverlayWindow)
    }
    if (useNativeOverlayWindow) {
        val inspector = ComposeUIViewController {
            io.heimdall.ui.HeimdallOverlay(controller = overlayController) {}
        }
        overlayWindow = PassthroughOverlayWindow(UIScreen.mainScreen.bounds, overlayController).also { window ->
            window.windowLevel = UIWindowLevelAlert + 1.0
            window.rootViewController = inspector
            window.hidden = false
        }
    }
    return ShakeHostViewController(content, shakeListener)
}
