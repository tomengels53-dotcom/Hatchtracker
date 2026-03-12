plugins {
    alias(libs.plugins.hatchtracker.android.library)
    alias(libs.plugins.hatchtracker.build.safety)
}

android {
    namespace = "com.example.hatchtracker.core.common"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:feature-access"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.hilt.android)
    implementation(libs.javax.inject)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
}
