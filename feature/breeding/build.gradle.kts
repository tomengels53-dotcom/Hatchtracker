plugins {
    alias(libs.plugins.hatchtracker.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.hatchtracker.feature.breeding"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
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
    implementation(project(":core:logging"))
    implementation(project(":core:feature-access"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.hilt.android)
    implementation(libs.javax.inject)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
