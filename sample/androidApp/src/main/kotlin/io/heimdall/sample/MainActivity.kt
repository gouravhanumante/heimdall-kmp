package io.heimdall.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.heimdall.core.AndroidShakeListener
import io.heimdall.ui.HeimdallOverlay
import io.heimdall.ui.HeimdallOverlayController

class MainActivity : ComponentActivity() {

    private lateinit var shakeListener: AndroidShakeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val controller = remember { HeimdallOverlayController() }
            shakeListener = remember { AndroidShakeListener(applicationContext) { controller.recall() } }

            MaterialTheme {
                HeimdallOverlay(controller = controller) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Heimdall sample app")
                    }
                }
            }
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
