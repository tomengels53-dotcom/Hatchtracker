plugins {
    alias(libs.plugins.hatchtracker.android.library)
    alias(libs.plugins.hatchtracker.build.safety)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.hatchtracker.core.ads"
}

dependencies {
    implementation(project(":core:billing"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:logging"))
    implementation(project(":core:model"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.play.services.ads)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    implementation(libs.javax.inject)
    ksp(libs.hilt.compiler)
}
