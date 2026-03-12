plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("hatchtracker.hilt")
}

android {
    namespace = "com.example.hatchtracker.core.notifications"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:data-local"))
    implementation(project(":core:logging"))
    implementation(project(":core:billing")) // only if notification gating uses subscription tier
    implementation(project(":core:data"))    // only if it reads repos; otherwise remove
    implementation(project(":core:ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.gson)

    // WorkManager for scheduled background work
    implementation(libs.androidx.work.runtime.ktx)
    implementation(platform(libs.firebase.bom))
    implementation("com.google.firebase:firebase-messaging")

    // If you use Hilt in workers/receivers
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.test:runner:1.6.2")
}
