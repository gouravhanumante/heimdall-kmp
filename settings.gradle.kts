rootProject.name = "heimdall-kmp"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":heimdall-core")
include(":heimdall-ui")
include(":heimdall-network-ktor")
include(":heimdall-storage")
include(":heimdall-flags-firebase")
include(":heimdall-database-sqlite")
include(":heimdall-core-noop")
include(":heimdall-ui-noop")
include(":heimdall-network-ktor-noop")

include(":sample:shared")
include(":sample:androidApp")
