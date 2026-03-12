plugins {
    id("hatchtracker.android.application")
    id("hatchtracker.hilt")
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.androidx.room)
    id("hatchtracker.android.guardrails")
}

android {
    namespace = "com.example.hatchtracker"

    defaultConfig {
        applicationId = "com.example.hatchtracker"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            buildConfigField("boolean", "ENABLE_CRASHLYTICS", "false")
            buildConfigField("String", "FIREBASE_ENV", "\"debug\"")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                abiFilters.add("arm64-v8a")
                abiFilters.add("armeabi-v7a")
            }
            buildConfigField("boolean", "ENABLE_CRASHLYTICS", "true")
            buildConfigField("String", "FIREBASE_ENV", "\"prod\"")
        }
        create("releaseStaging") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            // Harden staging to match production obfuscation/minification.
            isMinifyEnabled = true
            isShrinkResources = true
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "ENABLE_CRASHLYTICS", "true")
            buildConfigField("String", "FIREBASE_ENV", "\"staging\"")
        }
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }

    bundle {
        language {
            enableSplit = false
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
}

composeCompiler {
    if (project.findProperty("enableComposeCompilerMetrics") == "true") {
        metricsDestination = layout.buildDirectory.dir("compose_metrics")
        reportsDestination = layout.buildDirectory.dir("compose_metrics")
    }
}

room {
    // App module does not own Room entities; use the shared schema directory
    // so androidTest Room copy tasks have a valid input path.
    schemaDirectory("$rootDir/core/data-local/schemas")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data-local"))
    implementation(project(":core:data"))
    implementation(project(":core:billing"))
    implementation(project(":core:domain"))
    implementation(project(":core:di"))
    implementation(project(":core:logging"))
    implementation(project(":core:notifications"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":core:ads"))
    implementation(project(":core:common"))
    implementation(project(":core:scanner"))
    implementation(project(":feature:mainmenu"))
    implementation(project(":feature:production"))
    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    // Google Play Services
    implementation(libs.play.services.auth)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.messaging)


    implementation(libs.androidx.work.runtime.ktx)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.leakcanary.android)

    implementation(libs.kotlinx.coroutines.play.services)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.gson)
    implementation(libs.coil.compose)

    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.metrics.performance)
    implementation(libs.androidx.tracing.ktx)
    implementation(libs.androidx.profileinstaller)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.13")
}

ksp {
    arg("room.generateKotlin", "true")
}


// Alias 'testClasses' to the debug unit test task to satisfy tooling that expects this standard task.
tasks.register("testClasses") {
    dependsOn("testDebugUnitTest")
}

tasks.register("checkHardcodedStrings") {
    doLast {
        val hardcodedPatterns = listOf("Text(\"", "showSnackbar(\"", "Toast.makeText(.*, \"")
        val scanDir = file("src/main/java/com/example/hatchtracker")
        var found = false
        scanDir.walkTopDown().forEach { file ->
            if (file.extension == "kt" || file.extension == "java") {
                val content = file.readText()
                hardcodedPatterns.forEach { pattern ->
                    if (content.contains(pattern)) {
                        println("Hardcoded string found in ${file.absolutePath}: $pattern")
                        found = true
                    }
                }
            }
        }
        if (found) {
            println("WARNING: Hardcoded strings detected. Please use string resources.")
        }
    }
}


