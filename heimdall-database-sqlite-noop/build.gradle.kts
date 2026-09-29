plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates(group.toString(), "heimdall-database-sqlite-noop", version.toString())
    pom {
        name.set("Heimdall Database (SQLite) (no-op)")
        description.set("Release build stand-in for heimdall-database-sqlite: same public API, reads nothing.")
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
        namespace = "io.heimdall.database.sqlite.noop"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTest { isReturnDefaultValues = true }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.heimdallCoreNoop)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
