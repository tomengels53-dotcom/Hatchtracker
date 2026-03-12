plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.hatchtracker.kotlin.configuration)
    alias(libs.plugins.hatchtracker.build.safety)
    `java-library`
}

dependencies {
    api(libs.junit)
    api(libs.mockk)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    api(libs.javax.inject)
}
