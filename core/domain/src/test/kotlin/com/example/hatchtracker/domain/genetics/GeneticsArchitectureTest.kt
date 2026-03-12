package com.example.hatchtracker.domain.genetics

import org.junit.Test
import java.io.File
import java.util.Scanner

/**
 * Architectural tests for the Breeding & Genetics module.
 * Enforces the "Single Canonical Pipeline" and "Facade-only" rules.
 */
class GeneticsArchitectureTest {

    private val rootDir = File("../../") // Adjust based on test execution environment
    private val sourceDirs = listOf(
        "core/data/src/main/java",
        "core/domain/src/main/java",
        "core/domain/src/main/kotlin",
        "app/src/main/java"
    )

    private val bannedImports = listOf(
        "com.example.hatchtracker.domain.logic.GeneticProbabilityEngine",
        "com.example.hatchtracker.domain.logic.PhenotypeResolver",
        "com.example.hatchtracker.domain.breeding.StrategySearchEngine",
        "com.example.hatchtracker.domain.breeding.PlanScorer"
    )

    private val facadeOnlyClasses = listOf(
        "GeneticProbabilityEngine",
        "PhenotypeResolver",
        "LineStabilityIndexCalculator"
    )

    @Test
    fun `enforce no legacy genetics engine imports`() {
        val violations = mutableListOf<String>()

        sourceDirs.forEach { dirPath ->
            val dir = File(rootDir, dirPath)
            if (dir.exists()) {
                dir.walkTopDown().filter { it.extension == "kt" || it.extension == "java" }.forEach { file ->
                    val content = file.readText()
                    bannedImports.forEach { banned ->
                        if (content.contains(banned)) {
                            violations.add("Banned import '$banned' found in ${file.path}")
                        }
                    }
                }
            }
        }

        assert(violations.isEmpty()) { 
            "Architecture Violation: Legacy genetics engines are being imported!\n" + violations.joinToString("\n")
        }
    }

    @Test
    fun `enforce facade-only rule for genetics engines`() {
        val violations = mutableListOf<String>()

        sourceDirs.forEach { dirPath ->
            val dir = File(rootDir, dirPath)
            if (dir.exists()) {
                dir.walkTopDown().filter { file -> 
                    file.extension == "kt" && file.name != "BreedingPredictionService.kt" 
                }.forEach { file ->
                    val content = file.readText()
                    facadeOnlyClasses.forEach { className ->
                        // Check for direct calls like "GeneticProbabilityEngine.predict" 
                        // but ignore the imports (already handled) and references in comments.
                        // This is a simplified grep-based check.
                        val callPattern = Regex("""\b$className\.""")
                        if (callPattern.containsMatchIn(content)) {
                            violations.add("Direct call to '$className' found in ${file.path}. Use BreedingPredictionService instead.")
                        }
                    }
                }
            }
        }

        assert(violations.isEmpty()) { 
            "Architecture Violation: Genetics engines must only be accessed via BreedingPredictionService!\n" + violations.joinToString("\n")
        }
    }
}
