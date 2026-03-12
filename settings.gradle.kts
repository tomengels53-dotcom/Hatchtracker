pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "HatchTracker"
include(":app")
include(":macrobenchmark")
include(":core:model")
include(":core:data-local")
include(":core:data-remote")
include(":core:di")
include(":core:data")
include(":core:billing")
include(":core:notifications")

include(":core:domain")

include(":core:logging")
include(":core:common")
include(":core:ui")
include(":core:navigation")
include(":core:ads")
include(":core:feature-access")
include(":core:scanner")

include(":feature:bird")
include(":feature:flock")
include(":feature:incubation")
include(":feature:nursery")
include(":feature:production")
include(":feature:breeding")
include(":feature:finance")
include(":feature:devices")
include(":feature:support")
include(":feature:auth")
include(":feature:admin")
include(":feature:notifications")

include(":feature:profile")
include(":feature:mainmenu")
include(":feature:community")

