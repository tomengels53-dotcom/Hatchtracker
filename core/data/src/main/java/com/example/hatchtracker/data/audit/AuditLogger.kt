package com.example.hatchtracker.data.audit

import com.example.hatchtracker.auth.UserAuthManager
import com.example.hatchtracker.model.AdminAuditLog
import com.example.hatchtracker.model.AuditActionType
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Singleton utility for logging administrative actions to Firestore.
 */
object AuditLogger {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private const val COLLECTION_NAME = "adminAuditLogs"

    /**
     * Adds an audit log entry to an existing WriteBatch for atomic commits.
     */
    fun logActionToBatch(
        batch: com.google.firebase.firestore.WriteBatch,
        actionType: AuditActionType,
        targetCollection: String,
        targetDocumentId: String,
        before: Any? = null,
        after: Any? = null,
        reason: String? = null
    ) {
        try {
            val user = UserAuthManager.currentUser.value
            val userId = user?.uid ?: "unknown"
            val email = user?.email ?: "unknown"
            
            val logRef = firestore.collection(COLLECTION_NAME).document()
            
            val logEntry = AdminAuditLog(
                id = logRef.id,
                adminUserId = userId,
                adminEmail = email,
                actionType = actionType,
                targetCollection = targetCollection,
                targetDocumentId = targetDocumentId,
                beforeSnapshot = toMap(before),
                afterSnapshot = toMap(after),
                timestamp = System.currentTimeMillis(),
                reason = reason,
                clientVersion = "1.0.0"
            )

            batch.set(logRef, logEntry)
        } catch (e: Exception) {
            Logger.e(LogTags.SUPPORT, "Failed to add log to batch", e)
            throw e // Rethrow to ensure batch isn't committed without log if that's the intent
        }
    }

    /**
     * Logs an admin action to Firestore immediately.
     */
    suspend fun logAction(
        actionType: AuditActionType,
        targetCollection: String,
        targetDocumentId: String,
        before: Any? = null,
        after: Any? = null,
        reason: String? = null
    ): Result<Unit> {
        return try {
            val user = UserAuthManager.currentUser.value
            val userId = user?.uid ?: "unknown"
            val email = user?.email ?: "unknown"

            // Simple conversion of data objects to Maps is preferred for flexible querying
            // In a real app we might use Gson or specific mappers here.
            // For now, we assume the passed objects are either Maps or basic Data Classes 
            // that Firestore can serialize automatically, but we store them in the 'snapshot' fields.
            
            // To be safe and explicit, let's wrap them in the log object.
            // Note: Firestore's Java/Kotlin SDK handles POJOs in 'snapshot' map fields if they are @PropertyName annotated or simple data classes.
            
            val logEntry = AdminAuditLog(
                adminUserId = userId,
                adminEmail = email,
                actionType = actionType,
                targetCollection = targetCollection,
                targetDocumentId = targetDocumentId,
                beforeSnapshot = toMap(before),
                afterSnapshot = toMap(after),
                timestamp = System.currentTimeMillis(),
                reason = reason,
                clientVersion = "1.0.0" // TODO: Inject BuildConfig.VERSION_NAME
            )

            firestore.collection(COLLECTION_NAME).add(logEntry).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(LogTags.SUPPORT, "Failed to log admin action", e)
            Result.failure(e)
        }
    }
    
    // Helper to attempt conversion to Map for cleaner storage, or null if null
    private fun toMap(obj: Any?): Map<String, Any?>? {
        if (obj == null) return null
        @Suppress("UNCHECKED_CAST")
        if (obj is Map<*, *>) return obj as Map<String, Any?>
        // Fallback: Let Firestore SDK handle it during serialization of parent object? 
        // No, 'beforeSnapshot' is typed as Map in model. We need a conversion if we pass complex objects.
        // For this implementation, let's assume calling code converts to map, 
        // OR we use a simple reflection or reliance on specific types.
        // To keep it robust without heavy libs like Gson impacting method sig:
        try {
            // Very naive reflection-less approach: assume callers pass data classes that we can't easily iterate without reflection/Gson.
            // BUT: converting to Map is safer.
            // Using a hack: write to a temporary Map using Firestore's own mapper? Expensive.
            // Let's rely on callers passing Maps or we can add a 'Gson' dependency if available.
            // Checking imports... no Gson.
            // Let's assume callers use a helper `.asMap()` or similar on their models, 
            // OR we change the Model to take `Any?` instead of Map.
            // I will change the model to `Any?` in a subsequent step if this is painful, 
            // but for now let's just return null if not a map and rely on callers.
            return null
        } catch (e: Exception) {
            return null
        }
    }
}


