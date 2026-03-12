import com.android.build.api.dsl.LibraryExtension

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    id("com.android.library")
    id("hatchtracker.kotlin.configuration")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    compileSdk = libs.findVersion("compileSdk").get().toString().toInt()

    defaultConfig {
        minSdk = libs.findVersion("minSdk").get().toString().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

configure<org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension> {
    val stabilityConfig = rootProject.file("compose_compiler_config.conf")
    if (stabilityConfig.exists()) {
        stabilityConfigurationFile.set(stabilityConfig)
    }
}

dependencies {
    implementation(platform(libs.findLibrary("androidx-compose-bom").get()))
}

fun android(block: LibraryExtension.() -> Unit) {
    extensions.configure(LibraryExtension::class.java, block)
}
