plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates(group.toString(), "heimdall-core", version.toString())
    pom {
        name.set("Heimdall Core")
        description.set("Platform-agnostic core (sessions, collectors, events, screen attribution) for the Heimdall in-app debug inspector.")
        url.set("https://github.com/gouravhanumante/heimdall-kmp")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("gouravhanumante")
                name.set("Gourav Hanumante")
                url.set("https://github.com/gouravhanumante")
            }
        }
        scm {
            url.set("https://github.com/gouravhanumante/heimdall-kmp")
            connection.set("scm:git:git://github.com/gouravhanumante/heimdall-kmp.git")
            developerConnection.set("scm:git:ssh://git@github.com/gouravhanumante/heimdall-kmp.git")
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "io.heimdall.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTest { isReturnDefaultValues = true }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            api(libs.androidx.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidHostTest").dependencies {
            // The Android target's own bundled driver ships a device .so, unusable from the
            // plain JVM these tests actually run on — this pulls the real macOS/Linux native lib.
            implementation(libs.androidx.sqlite.bundled.jvm)
        }
    }
}
