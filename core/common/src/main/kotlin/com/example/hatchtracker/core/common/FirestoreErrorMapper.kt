package com.example.hatchtracker.core.common

import com.google.firebase.firestore.FirebaseFirestoreException
import com.example.hatchtracker.model.UiText
import com.example.hatchtracker.core.common.R

/**
 * Maps Firestore exceptions to friendly user messages.
 * Used to handle "Permission Denied" errors from strict security rules.
 */
object FirestoreErrorMapper {

    fun userMessage(t: Throwable?): UiText {
        if (t == null) return UiText.StringResource(R.string.error_unknown_occurred)

        val message = t.message ?: ""
        
        // Check for permission denied
        if (message.contains("PERMISSION_DENIED", ignoreCase = true) ||
            (t is FirebaseFirestoreException && t.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)) {
            return UiText.StringResource(R.string.error_permission_denied_settings)
        }

        // Check for unauthenticated
        if (message.contains("UNAUTHENTICATED", ignoreCase = true) ||
            (t is FirebaseFirestoreException && t.code == FirebaseFirestoreException.Code.UNAUTHENTICATED)) {
            return UiText.StringResource(R.string.error_unauthenticated_retry)
        }

        return UiText.StringResource(R.string.error_generic_something_went_wrong)
    }
}
