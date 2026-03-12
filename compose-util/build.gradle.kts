plugins {
    alias(libs.plugins.kotlin.compose)
    id("hatchtracker.android.library")
}

android {
    namespace = "com.example.hatchtracker.composeutil"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.play.services.ads)
}
