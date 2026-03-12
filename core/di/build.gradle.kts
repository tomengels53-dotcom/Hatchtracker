plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.hatchtracker.core.di"
    compileSdk = 36
    defaultConfig { minSdk = 24 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
    // Avoid intermittent missing generated class files (NoSuchFileException) in :core:di on Windows.
    options.isIncremental = false
}

dependencies {
    implementation(project(":core:model"))
    api(project(":core:data-local"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:billing"))
    implementation(project(":core:navigation"))
    implementation(project(":core:notifications"))

    implementation(libs.android.billingclient)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.functions)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // If your DI modules reference WorkManager:
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
}
