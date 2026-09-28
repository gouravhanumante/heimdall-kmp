package io.heimdall.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.heimdall.core.AndroidShakeListener
import io.heimdall.sample.shared.SampleApp
import io.heimdall.ui.HeimdallOverlayController

class MainActivity : ComponentActivity() {

    private lateinit var controller: HeimdallOverlayController
    private lateinit var shakeListener: AndroidShakeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        controller = HeimdallOverlayController()
        shakeListener = AndroidShakeListener(applicationContext) { controller.recall() }

        setContent {
            SampleApp(overlayController = controller)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::shakeListener.isInitialized) {
            shakeListener.start()
        }
    }

    override fun onPause() {
        if (::shakeListener.isInitialized) {
            shakeListener.stop()
        }
        super.onPause()
    }
}

