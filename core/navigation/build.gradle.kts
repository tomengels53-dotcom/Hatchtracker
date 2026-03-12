plugins {
    alias(libs.plugins.hatchtracker.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.hatchtracker.core.navigation"
}

dependencies {
    api(libs.androidx.navigation.compose)
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    
    implementation(libs.hilt.android)
    implementation(libs.javax.inject)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
}
