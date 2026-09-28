package io.heimdall.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import io.heimdall.core.AndroidShakeListener
import io.heimdall.sample.shared.SampleApp
import io.heimdall.ui.HeimdallOverlayController

class MainActivity : ComponentActivity() {

    private lateinit var shakeListener: AndroidShakeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val controller = remember { HeimdallOverlayController() }
            shakeListener = remember { AndroidShakeListener(applicationContext) { controller.recall() } }

            SampleApp(overlayController = controller)
        }
    }

    override fun onResume() {
        super.onResume()
        shakeListener.start()
    }

    override fun onPause() {
        shakeListener.stop()
        super.onPause()
    }
}

