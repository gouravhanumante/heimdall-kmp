plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates(group.toString(), "heimdall-storage", version.toString())
    pom {
        name.set("Heimdall Storage")
        description.set("DataStore/SharedPreferences/UserDefaults/Keystore/Keychain discovery for Heimdall.")
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
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "io.heimdall.storage"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTest { isReturnDefaultValues = true }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.heimdallCore)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.datastore.preferences.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.okio)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.androidx.sqlite.bundled.jvm)
        }
    }
}
