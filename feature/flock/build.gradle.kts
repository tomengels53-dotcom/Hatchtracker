plugins {
    alias(libs.plugins.hatchtracker.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.hatchtracker.feature.flock"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    buildFeatures {
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:di"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:data-local"))
    implementation(project(":core:billing"))
    implementation(project(":core:feature-access"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.compose)

    implementation(libs.hilt.android)
    implementation(libs.javax.inject)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
