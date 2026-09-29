plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "HeimdallSampleShared"
            isStatic = true
        }
    }

    androidLibrary {
        namespace = "io.heimdall.sample.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    sourceSets {
        // Xcode sets CONFIGURATION when it runs `embedAndSignAppleFrameworkForXcode` as a build
        // phase script (see sample/iosApp/project.yml); unset for the Android build and for
        // ad-hoc `./gradlew compileKotlinIos...` runs, which default to the real modules.
        val useNoop = System.getenv("CONFIGURATION") == "Release"

        commonMain.dependencies {
            // api, not implementation: sample:androidApp needs AndroidShakeListener and
            // HeimdallOverlayController directly, since shake-wiring is host-app code, not
            // something SampleApp() itself can own (it differs per platform).
            if (useNoop) {
                api(projects.heimdallCoreNoop)
                api(projects.heimdallUiNoop)
                implementation(projects.heimdallNetworkKtorNoop)
            } else {
                api(projects.heimdallCore)
                api(projects.heimdallUi)
                implementation(projects.heimdallNetworkKtor)
            }
            implementation(libs.androidx.sqlite.bundled)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}


composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose-compiler")
    metricsDestination = layout.buildDirectory.dir("compose-compiler")
}

compose.resources {
    publicResClass = true
    packageOfResClass = "io.heimdall.sample.shared.generated.resources"
}
