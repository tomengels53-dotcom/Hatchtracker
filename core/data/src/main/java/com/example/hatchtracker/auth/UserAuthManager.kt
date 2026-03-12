package com.example.hatchtracker.auth

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.example.hatchtracker.core.data.BuildConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await

/**
 * Manages user authentication state and operations.
 */
object UserAuthManager {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    // Role States
    private val _isSystemAdmin = MutableStateFlow(false)
    val isSystemAdmin: StateFlow<Boolean> = _isSystemAdmin.asStateFlow()

    private val _isDeveloper = MutableStateFlow(false)
    val isDeveloper: StateFlow<Boolean> = _isDeveloper.asStateFlow()

    private val _isCommunityAdmin = MutableStateFlow(false)
    val isCommunityAdmin: StateFlow<Boolean> = _isCommunityAdmin.asStateFlow()

    // Integrity Check: Track unauthorized admins
    private val _unauthorizedAdmins = MutableStateFlow<List<com.example.hatchtracker.domain.model.UserProfile>>(emptyList())
    val unauthorizedAdmins: StateFlow<List<com.example.hatchtracker.domain.model.UserProfile>> = _unauthorizedAdmins.asStateFlow()

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    private var authJob: kotlinx.coroutines.Job? = null

    init {
        // Listen for auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user

            authJob?.cancel()
            authJob = scope.launch {
                if (user == null) {
                    _isSystemAdmin.value = false
                    _isDeveloper.value = false
                    return@launch
                }

                try {
                    val tokenResult = user.getIdToken(false).await()
                    _isSystemAdmin.value = parseClaimBoolean(tokenResult.claims, "isAdmin") || parseClaimBoolean(tokenResult.claims, "isSystemAdmin")
                    _isDeveloper.value = parseClaimBoolean(tokenResult.claims, "isDeveloper")
                } catch (e: Exception) {
                    com.example.hatchtracker.core.logging.Logger.e(com.example.hatchtracker.core.logging.LogTags.AUTH, "Failed to fetch ID token claims", e)
                }
            }
        }
    }




    /**
     * Signs in with a Google credential.
     */
    suspend fun signInWithGoogle(credential: AuthCredential): Result<FirebaseUser?> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs out the current user.
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Signs in with email and password.
     */
    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser?> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a new account with email and password.
     */
    suspend fun signUpWithEmail(email: String, pass: String): Result<FirebaseUser?> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends a password reset email.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Links the current user with email credentials.
     */
    suspend fun linkWithEmail(email: String, pass: String): Result<FirebaseUser?> {
        val user = auth.currentUser
        return if (user != null && user.isAnonymous) {
            try {
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, pass)
                val result = user.linkWithCredential(credential).await()
                Result.success(result.user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            // If not anonymous or null, just sign in (or return error depending on desired flow)
            signInWithEmail(email, pass)
        }
    }

    /**
     * Links the current user with a generic credential (e.g. Google).
     */
    suspend fun linkWithCredential(credential: AuthCredential): Result<FirebaseUser?> {
        val user = auth.currentUser
        return if (user != null && user.isAnonymous) {
            try {
                val result = user.linkWithCredential(credential).await()
                Result.success(result.user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            signInWithCredential(credential)
        }
    }

    /**
     * Private helper for credential sign-in
     */
    private suspend fun signInWithCredential(credential: AuthCredential): Result<FirebaseUser?> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reloads the current user's session data.
     * Useful for checking strict validity (e.g. account disabled status).
     */
    suspend fun reloadSession(): Result<Unit> {
        return try {
            auth.currentUser?.reload()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns a flow of the current user state.
     */
    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    private fun parseClaimBoolean(claims: Map<String, Any>, key: String): Boolean {
        val value = claims[key] ?: return false
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value.equals("true", ignoreCase = true) || value == "1"
            else -> false
        }
    }
}
