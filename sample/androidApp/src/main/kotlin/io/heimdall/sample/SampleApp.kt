package io.heimdall.sample

import android.app.Application
import io.heimdall.core.Heimdall
import io.heimdall.core.PlatformContext
import io.heimdall.storage.discoverStorage

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Heimdall.install(PlatformContext(this))
        getSharedPreferences("sample_preferences", MODE_PRIVATE)
            .edit()
            .putString("last_action", "Sample app started")
            .putInt("age", 28)
            .putBoolean("notifications", true)
            .putFloat("height", 1.75f)
            .putLong("points", 100L)
            .apply()
        Heimdall.discoverStorage(this)

    }
}
