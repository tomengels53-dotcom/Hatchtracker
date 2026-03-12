package com.example.hatchtracker.app

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Static analysis test to enforce the strict performance rules applied to app startup.
 * Specifically enforcing P0 (No heavy work in Application.onCreate) and P1 (No main thread blocking).
 */
class StartupPerformanceInvariantTest {

    @Test
    fun `enforce no new blocking work in HatchApplication onCreate`() {
        // Read HatchApplication file directly from source for static analysis
        val applicationFile = File("src/main/java/com/example/hatchtracker/app/HatchApplication.kt")
        if (!applicationFile.exists()) {
            fail("Could not find HatchApplication.kt at ${applicationFile.absolutePath}")
        }

        val content = applicationFile.readText()
        val onCreateMatch = Regex(
            """override\s+fun\s+onCreate\(\)\s*\{(.*)androidx\.tracing\.Trace\.endSection\(\)\s*//\s*End\s*HatchApplication\.onCreate""",
            RegexOption.DOT_MATCHES_ALL
        ).find(content)
            ?: Regex("""override\s+fun\s+onCreate\(\)\s*\{([^}]*)\}""").find(content)
            ?: throw AssertionError("Could not locate onCreate in HatchApplication")

        val onCreateBody = onCreateMatch.value
        
        // Split body into two sections: synchronous UI thread operations and background scoped operations
        val scopedLaunchIndex = onCreateBody.indexOf("applicationScope.launch")
        assertTrue("Must defer background work using applicationScope.launch", scopedLaunchIndex > 0)
        
        val syncOnCreatePart = onCreateBody.substring(0, scopedLaunchIndex)
        val asyncOnCreatePart = onCreateBody.substring(scopedLaunchIndex)

        // Rule 1: No synchronous disk, network, or heavy init on the main thread
        val forbiddenSyncCalls = listOf(
            "runBlocking",
            "Thread.sleep",
            ".get()", // Eager dagger loading is restricted, should be lazy or deferred
            "WorkManager.getInstance", // This initializes WorkManager DB, must be async
            "FirebaseFirestore.getInstance",
            "SharedPreferences"
        )
        
        forbiddenSyncCalls.forEach { badCall ->
            if (syncOnCreatePart.contains(badCall)) {
                // If it's `get()` for integrityDebugRunner inside the launch blocks it's okay, 
                // but checking the sync part specifically enforcing the rule.
                fail("Violation (P0/P1): Synchronous part of onCreate contains forbidden call: ${badCall}. Move it to applicationScope.launch(Dispatchers.IO)")
            }
        }
        
        // Rule 2: Ensure background work is still using Dispatchers.IO
        assertTrue("Background tasks in onCreate must run on Dispatchers.IO", 
            asyncOnCreatePart.contains("launch(Dispatchers.IO)"))
        
        // Rule 3: Ensure StrictMode is maintained
        assertTrue("StrictMode VM and Thread policies must be configured for debug builds to catch blocking calls", 
            syncOnCreatePart.contains("android.os.StrictMode.setThreadPolicy") && 
            syncOnCreatePart.contains("android.os.StrictMode.setVmPolicy"))
            
        // Rule 4: Ensure trace blocks are intact
        assertTrue("Trace block for Application onCreate must exist", syncOnCreatePart.contains("androidx.tracing.Trace.beginSection(\"HatchApplication.onCreate\")"))
    }
}
