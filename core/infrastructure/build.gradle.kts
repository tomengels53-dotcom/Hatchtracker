plugins {
    alias(libs.plugins.hatchtracker.android.library)
    alias(libs.plugins.hatchtracker.build.safety)
}

android {
    namespace = "com.example.hatchtracker.core.infrastructure"
}

dependencies {
    implementation(project(":core:common"))
}
