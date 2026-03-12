import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// This plugin DOES NOT apply Kotlin; it only configures it if a platform plugin is present.
plugins.withType<org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper> {
    // Configures the 'kotlin' extension registered by the module's platform plugin (android or jvm).
    // Note: We use the 'afterEvaluate' or specific configuration logic to ensure the extension exists.
    configure<org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension> {
        jvmToolchain(17)
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
