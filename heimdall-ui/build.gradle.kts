plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "HeimdallUi"
            isStatic = true
        }
    }

    androidLibrary {
        namespace = "io.heimdall.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true
        withHostTest { isReturnDefaultValues = true }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.heimdallCore)
            implementation(libs.compose.runtime)
            implementation(libs.compose.animation)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
                implementation(libs.compose.resources)
        }
    }
}

    compose.resources {
        publicResClass = true
        packageOfResClass = "io.heimdall.ui.generated.resources"
    }

    composeCompiler {
        reportsDestination = layout.buildDirectory.dir("compose-compiler")
        metricsDestination = layout.buildDirectory.dir("compose-compiler")
    }
