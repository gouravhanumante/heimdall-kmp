plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates(group.toString(), "heimdall-noop", version.toString())
    pom {
        name.set("Heimdall No-op")
        description.set("Complete no-op Heimdall bundle for release variants.")
        url.set("https://github.com/gouravhanumante/heimdall-kmp")
        licenses { license { name.set("Apache-2.0"); url.set("https://www.apache.org/licenses/LICENSE-2.0.txt") } }
        developers { developer { id.set("gouravhanumante"); name.set("Gourav Hanumante"); url.set("https://github.com/gouravhanumante") } }
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
    compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }

    androidLibrary {
        namespace = "io.heimdall"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.heimdallCoreNoop)
            api(projects.heimdallUiNoop)
            api(projects.heimdallNetworkKtorNoop)
            api(projects.heimdallStorageNoop)
            api(projects.heimdallDatabaseSqliteNoop)
        }
        androidMain.dependencies {
            api(projects.heimdallFlagsFirebaseNoop)
        }
    }
}
