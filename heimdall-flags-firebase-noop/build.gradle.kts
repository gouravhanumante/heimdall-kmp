plugins {
    id("com.android.library")
}

android {
    namespace = "io.heimdall.flags.firebase.noop"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

dependencies {
    api(projects.heimdallCoreNoop)
    api(libs.firebase.config)
}
