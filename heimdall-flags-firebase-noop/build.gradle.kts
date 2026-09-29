plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    // Empty, not Dokka-generated: Dokka can't read FlagValue's sealed-class bytecode
    // ("PermittedSubclasses requires ASM9") on this Kotlin/JDK combination.
    configure(com.vanniktech.maven.publish.AndroidSingleVariantLibrary(
        javadocJar = com.vanniktech.maven.publish.JavadocJar.Empty(),
        sourcesJar = com.vanniktech.maven.publish.SourcesJar.Sources(),
        variant = "release",
    ))
    coordinates(group.toString(), "heimdall-flags-firebase-noop", version.toString())
    pom {
        name.set("Heimdall Flags (Firebase) (no-op)")
        description.set("Release build stand-in for heimdall-flags-firebase: same public API, no override store.")
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
