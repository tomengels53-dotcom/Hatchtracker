package com.example.hatchtracker.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val database: com.example.hatchtracker.data.AppDatabase
) {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Initializing)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            handleAuthStateChange(user)
        }
    }

    private fun handleAuthStateChange(user: FirebaseUser?) {
        val currentState = _sessionState.value
        
        // Map Firebase User to SessionState
        val newState = if (user != null) {
            // We have a user, check if we were previously revoked/disabled to handle transitions
            // For now, simple mapping:
             SessionState.Authenticated(user)
        } else {
            // User is null.
            if (currentState is SessionState.LoggedOut) {
                SessionState.LoggedOut
            } else if (currentState is SessionState.Authenticated) {
                 SessionState.Unauthenticated
            } else {
                 SessionState.Unauthenticated
            }
        }
        
        // Emit new state if different
        if (currentState != newState) {
            _sessionState.value = newState
        }
    }

    fun signOut() {
        _sessionState.value = SessionState.LoggedOut
        auth.signOut()

        // Clear local database to ensure GDPR compliance ("right to erasure" applies locally too)
        scope.launch {
            try {
                database.clearAllTables()
            } catch (e: Exception) {
                // Log but don't crash the logout flow if local DB fails to clear
            }
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun updatePassword(current: String, new: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("User not logged in")
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, current)
            user.reauthenticate(credential).await()
            user.updatePassword(new).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun refreshSession() {
        scope.launch {
            try {
                auth.currentUser?.reload()?.addOnSuccessListener {
                    // Success, state listener will pick up any changes if needed, 
                    // or we stay matched.
                }?.addOnFailureListener { e ->
                    // Handle specific errors like user disabled
                     if (e is com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                         _sessionState.value = SessionState.Disabled
                     }
                }
            } catch (e: Exception) {
                // Generic error
            }
        }
    }
}
