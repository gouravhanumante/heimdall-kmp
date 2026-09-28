plugins {
    id("com.android.library")
}

android {
    namespace = "io.heimdall.flags.firebase"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

dependencies {
    api(projects.heimdallCore)
    api(libs.firebase.config)
}