plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.hatchtracker.kotlin.configuration)
    alias(libs.plugins.hatchtracker.build.safety)
    `java-library`
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
