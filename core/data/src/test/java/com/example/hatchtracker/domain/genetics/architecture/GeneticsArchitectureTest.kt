package com.example.hatchtracker.domain.genetics.architecture

import org.junit.Test
import java.io.File
import org.junit.Assert.assertTrue

/**
 * ENFORCEMENT SUITE: Ensures no regressions in the Breeding/Genetics architecture.
 * Rules:
 * 1. Only BreedingPredictionService may call internal Probability/Phenotype engines.
 * 2. No direct usage of 'predictOffspringTraits' (legacy).
 * 3. No direct usage of 'domain.logic' (v1 engine).
 */
class GeneticsArchitectureTest {
    @Test
    fun `Facade Rule - Only BreedingPredictionService should import internal engines`() {
        val domainSrc = findSourceDirectory("core/domain/src/main/java") ?: return
        
        val forbiddenImports = listOf(
            "com.example.hatchtracker.domain.genetics.engine.GeneticProbabilityEngine",
            "com.example.hatchtracker.domain.genetics.phenotype.PhenotypeResolver"
        )

        domainSrc.walkTopDown()
            .filter { it.extension == "kt" && !it.name.contains("BreedingPredictionService") && !it.name.contains("GeneticsFacade") }
            .forEach { file ->
                val content = file.readText()
                forbiddenImports.forEach { forbidden ->
                    assertTrue(
                        "Violation in ${file.name}: Direct import of $forbidden is forbidden. Use BreedingPredictionService.",
                        !content.contains("import $forbidden") && !content.contains(forbidden.substringAfterLast("."))
                    )
                }
            }
    }

    @Test
    fun `Legacy Cleanup - No references to deleted predictOffspringTraits`() {
        val srcDir = findSourceDirectory("src/main/java") ?: return
        
        srcDir.walkTopDown()
            .filter { it.extension == "kt" }
            .forEach { file ->
                val content = file.readText()
                assertTrue(
                    "Violation in ${file.name}: References deprecated 'predictOffspringTraits'. Use 'BreedingPredictionService.predictBreeding'.",
                    !content.contains("predictOffspringTraits")
                )
            }
    }

    @Test
    fun `CrossBreedingIntelligenceEngine - No hardcoded breeds`() {
        val file = findFile("core/domain/src/main/java/com/example/hatchtracker/domain/breeding/CrossBreedingIntelligenceEngine.kt")
        if (file == null || !file.exists()) return

        val content = file.readText()
        val forbiddenLiterals = listOf("Rhode Island Red", "Plymouth Rock", "Barred")
        
        forbiddenLiterals.forEach { literal ->
            assertTrue(
                "CrossBreedingIntelligenceEngine contains hardcoded breed string '$literal'. Strategy should be dynamic.",
                !content.contains("\"$literal\"")
            )
        }
    }

    private fun findSourceDirectory(pathSuffix: String): File? {
        // Try various common roots
        val roots = listOf(
            File("."), 
            File(".."), 
            File("../../..")
        )
        
        for (root in roots) {
            val potential = File(root, pathSuffix)
            if (potential.exists() && potential.isDirectory) return potential
        }
        return null
    }

    private fun findFile(path: String): File? {
        val roots = listOf(File("."), File(".."), File("../../.."))
        for (root in roots) {
            val potential = File(root, path)
            if (potential.exists()) return potential
        }
        return null
    }
}
