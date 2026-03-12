package com.example.hatchtracker.domain.breeding

import org.junit.Test
import org.junit.Assert.assertTrue
import java.io.File

/**
 * Ensures code follows the new architecture rules and prevents regression
 * via legacy component usage or domain bypasses.
 */
class ArchitectureEnforcementTest {

    // Assuming Gradle test execution with CWD = module root (core/domain)
    private val projectRoot = File("../..")
    private val projectRootPath = projectRoot.canonicalFile.toPath()

    private fun File.isProjectSourceFile(): Boolean {
        val normalized = canonicalPath.replace('\\', '/')
        val relative = normalizedRelativePath()
        val topLevelDir = relative.substringBefore('/', missingDelimiterValue = relative)
        val includedTopLevelDirs = setOf("app", "core", "feature", "compose-util")
        return (extension == "kt" || extension == "java") &&
            normalized.contains("/src/") &&
            !normalized.contains("/build/") &&
            !normalized.contains("/generated/") &&
            topLevelDir in includedTopLevelDirs
    }

    private fun File.normalizedRelativePath(): String {
        return projectRootPath.relativize(canonicalFile.toPath()).toString().replace('\\', '/')
    }

    @Test
    fun `Verify Legacy Components are Isolated`() {
        val sourceFiles = projectRoot.walkTopDown().filter { it.isProjectSourceFile() }
        
        val forbiddenImports = listOf(
            "com.example.hatchtracker.domain.breeding.CrossBreedingIntelligenceEngine",
            "com.example.hatchtracker.domain.breeding.TraitMatrix",
            "com.example.hatchtracker.domain.breeding.GeneticsFacade"
        )
        
        val allowedFiles = listOf(
            "CrossBreedingIntelligenceEngine.kt",
            "TraitMatrix.kt",
            "BreedingAnalysisModule.kt",
            "BreedingIntelligenceService.kt",
            "ArchitectureEnforcementTest.kt"
        )

        val violations = mutableListOf<String>()

        sourceFiles.forEach { file ->
            if (file.name !in allowedFiles) {
                val content = try { file.readText() } catch (e: Exception) { "" }
                forbiddenImports.forEach { forbidden ->
                    if (content.contains("import $forbidden")) {
                        violations.add("${file.normalizedRelativePath()} -> uses $forbidden")
                    }
                }
            }
        }

        assertTrue(
            "Legacy components used in modern code (delete these imports and use BreedingPredictionService):\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun `Verify Domain Logic Package Access`() {
        // Enforce that feature modules should use public interfaces (Repository/Service)
        // rather than internal domain logic bypasses.
        val sourceFiles = projectRoot.walkTopDown()
            .filter { it.isProjectSourceFile() }
            .filter { it.normalizedRelativePath().startsWith("feature/") && it.extension == "kt" }

        // Existing files still using domain.logic during migration.
        // Keep this list stable and fail on any new direct domain.logic imports.
        val migrationAllowList = emptySet<String>()

        val forbiddenPrefix = "import com.example.hatchtracker.domain.logic"
        val violations = mutableListOf<String>()

        sourceFiles.forEach { file ->
            val relativePath = file.normalizedRelativePath()
            if (file.readText().contains(forbiddenPrefix) && relativePath !in migrationAllowList) {
                violations.add(relativePath)
            }
        }

        assertTrue(
            "Feature modules MUST NOT add new imports to internal domain logic. Use Repository/Service layers:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun `Verify UI String Localization`() {
        val sourceFiles = projectRoot.walkTopDown()
            .filter { it.isProjectSourceFile() }
            .filter {
                val path = it.normalizedRelativePath()
                path.contains("/src/main/java/") && (path.contains("feature") || path.contains("ui"))
            }

        // Existing hardcoded UI strings under migration.
        // This test should fail only when new files introduce additional violations.
        val migrationAllowList = setOf(
            "core/ui/src/main/java/com/example/hatchtracker/core/ui/components/GeneticTraitPicker.kt",
            "core/ui/src/main/java/com/example/hatchtracker/core/ui/components/ThemeToggle.kt",
            "feature/admin/src/main/java/com/example/hatchtracker/feature/admin/AdminAuditLogScreen.kt",
            "feature/auth/src/main/java/com/example/hatchtracker/feature/auth/WelcomeScreen.kt",
            "feature/bird/src/main/java/com/example/hatchtracker/feature/bird/AddBirdScreen.kt",
            "feature/bird/src/main/java/com/example/hatchtracker/feature/bird/BirdListScreen.kt",
            "feature/devices/src/main/java/com/example/hatchtracker/feature/devices/AddDeviceScreen.kt",
            "feature/incubation/src/main/java/com/example/hatchtracker/feature/incubation/AddIncubationScreen.kt",
            "feature/incubation/src/main/java/com/example/hatchtracker/feature/incubation/TroubleshootingScreen.kt",
            "feature/finance/src/main/java/com/example/hatchtracker/feature/finance/AddFinancialEntryScreen.kt",
            "feature/flock/src/main/java/com/example/hatchtracker/feature/flock/ui/screens/FlockDetailScreen.kt",
            "feature/flock/src/main/java/com/example/hatchtracker/feature/flock/ui/screens/FlockletCard.kt",
            "feature/incubation/src/main/java/com/example/hatchtracker/feature/incubation/FinancialInsightsCard.kt",
            "feature/incubation/src/main/java/com/example/hatchtracker/feature/incubation/IncubationListScreen.kt",
            "feature/incubation/src/main/java/com/example/hatchtracker/feature/incubation/IncubationTimelineScreen.kt",
            "feature/mainmenu/src/main/java/com/example/hatchtracker/feature/mainmenu/screens/MainMenuScreen.kt",
            "feature/nursery/src/main/java/com/example/hatchtracker/feature/nursery/FlockletCard.kt",
            "feature/nursery/src/main/java/com/example/hatchtracker/feature/nursery/FlockletDetailScreen.kt"
        )

        // Match Text(text = "...")
        // We look for at least 3 characters to avoid flagging trivial empty strings or icons
        val textRegex = """Text\(\s*text\s*=\s*"([^"]{3,})"""".toRegex()
        val violations = mutableListOf<String>()

        sourceFiles.forEach { file ->
            val relativePath = file.normalizedRelativePath()
            val content = file.readText()
            textRegex.findAll(content).forEach { match ->
                if (relativePath !in migrationAllowList) {
                    violations.add("$relativePath -> \"${match.groupValues[1]}\"")
                }
            }
        }

        assertTrue(
            "New hardcoded UI strings found. Move these to strings.xml and use stringResource():\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }
}
