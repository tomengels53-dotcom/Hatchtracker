
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class ApkGuardrailTask : DefaultTask() {
    @get:InputFile
    abstract val artifactFile: RegularFileProperty

    @get:Input
    abstract val maxMb: Property<Double>

    @TaskAction
    fun execute() {
        val file = artifactFile.get().asFile
        val sizeMb = file.length() / (1024.0 * 1024.0)
        val limit = maxMb.get()
        
        println("-------------------------------------------------------")
        println("[RELEASE GUARDRAIL] Artifact: ${file.name}")
        println("[RELEASE GUARDRAIL] Size: ${"%.2f".format(sizeMb)} MB")
        println("[RELEASE GUARDRAIL] Limit: ${"%.2f".format(limit)} MB")
        println("-------------------------------------------------------")

        if (sizeMb > limit) {
            throw RuntimeException("Build failed: APK/AAB size (${"%.2f".format(sizeMb)} MB) exceeds guardrail limit ($limit MB)")
        }
    }
}

val androidComponents = extensions.findByType(ApplicationAndroidComponentsExtension::class.java)

androidComponents?.onVariants { variant ->
    if (variant.name == "release" || variant.name == "releaseStaging") {
        val capitalizedName = variant.name.replaceFirstChar { it.uppercase() }
        
        // Task to check APK size after assemble
        tasks.register<ApkGuardrailTask>("check${capitalizedName}ApkGuardrail") {
            group = "verification"
            description = "Checks if the release APK exceeds size limits."
            maxMb.set(25.0) // Set limit to 25MB for this app context
            
            // This is a simplified way to get the output file. 
            // In a full CI setup, we'd use the artifact API more robustly.
            val apkFile = File(project.buildDir, "outputs/apk/${variant.name}/app-${variant.name}.apk")
            artifactFile.set(apkFile)
            
            dependsOn("assemble${capitalizedName}")
        }

        // Task to check AAB size after bundle
        tasks.register<ApkGuardrailTask>("check${capitalizedName}BundleGuardrail") {
            group = "verification"
            description = "Checks if the release AAB exceeds size limits."
            maxMb.set(15.0) // AABs are smaller
            
            val aabFile = File(project.buildDir, "outputs/bundle/${variant.name}/app-${variant.name}.aab")
            artifactFile.set(aabFile)
            
            dependsOn("bundle${capitalizedName}")
        }
    }
}
