plugins {
    `kotlin-dsl`
}

group = "com.example.hatchtracker.buildlogic"

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.hilt.gradle.plugin)
    implementation(libs.ksp.gradle.plugin)
    implementation(libs.kotlin.compose.gradle.plugin)
    implementation(libs.androidx.baselineprofile.gradle)
}
