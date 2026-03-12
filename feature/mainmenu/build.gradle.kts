plugins {
    alias(libs.plugins.hatchtracker.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.hatchtracker.feature.mainmenu"
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
    implementation(project(":core:billing"))
    implementation(project(":core:ads"))
    implementation(project(":core:notifications"))
    implementation(project(":core:feature-access"))
    implementation(project(":core:scanner"))

    implementation(project(":feature:auth"))
    implementation(project(":feature:bird"))
    implementation(project(":feature:flock"))
    implementation(project(":feature:incubation"))
    implementation(project(":feature:nursery"))
    implementation(project(":feature:breeding"))
    implementation(project(":feature:finance"))
    implementation(project(":feature:production"))
    implementation(project(":feature:devices"))
    implementation(project(":feature:support"))
    implementation(project(":feature:admin"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:community"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.play.services.ads)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    implementation(libs.javax.inject)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
