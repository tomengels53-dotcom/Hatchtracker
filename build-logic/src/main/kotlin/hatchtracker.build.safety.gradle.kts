// This plugin enforces the project's architectural integrity by verifying plugin applications.
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

val kotlinPlatformPlugins = listOf(
    "org.jetbrains.kotlin.android",
    "org.jetbrains.kotlin.jvm",
    "org.jetbrains.kotlin.multiplatform",
    "org.jetbrains.kotlin.js"
)

allprojects {
    // Rule 1: Prevent Kotlin application in allprojects/subprojects blocks early.
    // We check if the plugin list already contains Kotlin before modules even start their own evaluation.
    if (this != rootProject && (plugins.hasPlugin("org.jetbrains.kotlin.android") || plugins.hasPlugin("org.jetbrains.kotlin.jvm"))) {
         // This is a bit tricky to catch mid-application via allprojects block vs module block.
         // A better way is to check the 'source' of the plugin application if possible, 
         // but Gradle doesn't make that easy. Instead, we enforce the module-level authority.
    }

    afterEvaluate {
        // Guard 1: Prevent Kotlin application in Root Project
        if (this == rootProject && (plugins.hasPlugin("org.jetbrains.kotlin.android") || plugins.hasPlugin("org.jetbrains.kotlin.jvm"))) {
            throw GradleException(
                "\n[BUILD SAFETY GUARD] VIOLATION: Kotlin platform plugin applied to the ROOT project.\n" +
                "RULE: Kotlin must be defined with 'apply false' in the root project and applied ONLY in modules.\n" +
                "OFFENDING PROJECT: ${project.path}\n"
            )
        }

        // Guard 2: Enforce "Exactly One Kotlin Plugin" for Platform
        val appliedPlatforms = kotlinPlatformPlugins.filter { plugins.hasPlugin(it) }
        if (appliedPlatforms.size > 1) {
            throw GradleException(
                "\n[BUILD SAFETY GUARD] VIOLATION: Multiple Kotlin platform plugins applied in module ${project.path}.\n" +
                "FOUND: ${appliedPlatforms.joinToString(", ")}\n" +
                "RULE: A module must apply EXACTLY ONE platform plugin.\n" +
                "FIX: Remove the redundant plugin declaration in ${project.path}/build.gradle.kts.\n"
            )
        }

        // Guard 3: Collision Detection for 'kotlin' extension
        // If an extension named 'kotlin' exists, ensure it wasn't registered by multiple sources.
        // In Gradle 8+, trying to register the same name twice throws an error immediately, 
        // but we can check for multiple plugins that are known to register it.
        // NOTE: kotlin.plugin.compose is a COMPILER plugin in Kotlin 2.0+ and does NOT register the extension.
        val extensionRegistrants = listOf(
            "org.jetbrains.kotlin.android",
            "org.jetbrains.kotlin.jvm"
        )
        val activeRegistrants = extensionRegistrants.filter { plugins.hasPlugin(it) }
        if (activeRegistrants.size > 1) {
             throw GradleException(
                "\n[BUILD SAFETY GUARD] VIOLATION: Multiple plugins attempting to register the 'kotlin' extension in ${project.path}.\n" +
                "OFFENDERS: ${activeRegistrants.joinToString(", ")}\n" +
                "RULE: Only the platform plugin (android/jvm) should register the 'kotlin' extension.\n"
            )
        }

        // Guard 4: Enforce Version Catalog
        configurations.all {
            resolutionStrategy.eachDependency {
                if (requested.group == "org.jetbrains.kotlin" && (requested.name == "kotlin-gradle-plugin")) {
                    val catalogVersion = libs.findVersion("kotlin").get().toString()
                    if (requested.version != null && requested.version != catalogVersion) {
                        throw GradleException(
                            "\n[BUILD SAFETY GUARD] VIOLATION: Kotlin version mismatch in module ${project.path}.\n" +
                            "RULE: All Kotlin plugins must use the version defined in libs.versions.toml ($catalogVersion).\n" +
                            "FOUND: '${requested.version}'. Likely caused by legacy kotlin(\"...\") syntax or hardcoded versions.\n"
                        )
                    }
                }
            }
        }
    }
}
