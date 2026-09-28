package io.heimdall.sample.shared

import io.heimdall.core.Heimdall
import io.heimdall.core.IosShakeListener
import io.heimdall.core.PlatformContext
import io.heimdall.ui.HeimdallOverlayController
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIEvent
import platform.UIKit.UIEventSubtype
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowLevelAlert
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewController
import platform.UIKit.UIScreen
import platform.CoreGraphics.CGPoint

private class PassthroughOverlayWindow(
    frame: platform.CoreGraphics.CGRect,
    private val controller: HeimdallOverlayController,
) : UIWindow(frame) {
    override fun hitTest(point: CGPoint, withEvent: UIEvent?): platform.UIKit.UIView? {
        if (!controller.acceptsOverlayTouch(point.x.toFloat(), point.y.toFloat())) return null
        return super.hitTest(point, withEvent)
    }
}

private var overlayWindow: PassthroughOverlayWindow? = null

private class ShakeHostViewController(
    private val content: UIViewController,
    private val shakeListener: IosShakeListener,
) : UIViewController(nibName = null, bundle = null) {
    override fun viewDidLoad() {
        super.viewDidLoad()
        addChildViewController(content)
        content.view.frame = view.bounds
        content.view.autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        view.addSubview(content.view)
        content.didMoveToParentViewController(this)
    }

    override fun motionEnded(motion: UIEventSubtype, withEvent: UIEvent?) {
        if (motion == UIEventSubtype.UIEventSubtypeMotionShake) {
            shakeListener.motionEndedWithShake()
        }
        super.motionEnded(motion, withEvent)
    }
}

/** Hosts [SampleApp] with the shared Compose overlay; shake events are forwarded to the controller. */
fun MainViewController(): UIViewController = createMainViewController(useNativeOverlayWindow = false)

/** Opt-in: the bubble lives in a separate UIWindow so it stays above native screens and dialogs. Unverified on device. */
fun MainViewControllerWithNativeOverlayWindow(): UIViewController =
    createMainViewController(useNativeOverlayWindow = true)

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
