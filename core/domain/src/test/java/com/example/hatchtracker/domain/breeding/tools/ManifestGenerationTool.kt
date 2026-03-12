package com.example.hatchtracker.domain.breeding.tools

import com.example.hatchtracker.domain.breeding.GeneticLociManifestGenerator
import org.junit.Test
import java.io.File

class ManifestGenerationTool {

    @Test
    fun `generate manifest file`() {
        val generator = GeneticLociManifestGenerator()
        val json = generator.generateManifestJson()
        
        // CWD is typically core/domain during test execution
        // We want project_root/scripts/generated
        // core/domain -> core -> project_root = 2 levels up
        
        val projectRoot = File("../..")
        val scriptsDir = File(projectRoot, "scripts/generated")
        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs()
        }
        
        val outputFile = File(scriptsDir, "genetic_loci_manifest.json")
        outputFile.writeText(json)
        
        println("Manifest generated at: ${outputFile.absolutePath}")
        println("Generated content length: ${json.length} characters")
    }
}
