plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.example.hatchtracker.benchmark"
    compileSdk = libs.versions.compileSdk.get().toInt()

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // This build type is used for macrobenchmarking
        create("benchmark") {
            isDebuggable = true
            signingConfig = getByName("debug").signingConfig
            matchingFallbacks += listOf("release")
        }
    }

}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.core.ktx)
}

tasks.matching { it.name == "connectedDebugAndroidTest" }.configureEach {
    enabled = false
}
