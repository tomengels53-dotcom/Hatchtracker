package com.example.hatchtracker.data.validation

import android.util.Log
import com.example.hatchtracker.core.data.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntegrityDebugRunner @Inject constructor(
    private val validator: DataIntegrityValidator
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Initializes background validation on startup for Debug builds.
     */
    fun onAppStartup() {
        if (BuildConfig.DEBUG) {
            triggerValidation("Startup")
        }
    }

    /**
     * Manual trigger for validation.
     * Can be called from hidden debug menus or admin actions.
     */
    fun triggerValidation(source: String) {
        scope.launch {
            Log.d("IntegrityRunner", "Starting data integrity check (Source: $source)...")
            try {
                val issues = validator.validateEverything()
                if (issues.isEmpty()) {
                    Log.i("IntegrityRunner", "✅ Data integrity check PASSED.")
                } else {
                    Log.e("IntegrityRunner", "❌ Data integrity check FAILED with ${issues.size} issues:")
                    issues.forEach { issue ->
                        when (issue.severity) {
                            DataIntegrityValidator.Severity.CRITICAL -> Log.e("IntegrityRunner", "[CRITICAL] ${issue.category}: ${issue.message} (Entity: ${issue.entityId})")
                            DataIntegrityValidator.Severity.MAJOR -> Log.w("IntegrityRunner", "[MAJOR] ${issue.category}: ${issue.message} (Entity: ${issue.entityId})")
                            DataIntegrityValidator.Severity.MINOR -> Log.d("IntegrityRunner", "[MINOR] ${issue.category}: ${issue.message} (Entity: ${issue.entityId})")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("IntegrityRunner", "Validator crashed during execution", e)
            }
        }
    }
}
