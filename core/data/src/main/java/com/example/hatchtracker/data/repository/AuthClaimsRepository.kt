package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.models.UserClaims
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for monitoring and refreshing Firebase Auth custom claims.
 */
@Singleton
class AuthClaimsRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isDeveloper = MutableStateFlow(false)
    val isDeveloper: StateFlow<Boolean> = _isDeveloper.asStateFlow()

    private val _isCommunityAdmin = MutableStateFlow(false)
    val isCommunityAdmin: StateFlow<Boolean> = _isCommunityAdmin.asStateFlow()

    init {
        // Keep role claims hot without requiring any Flow collector.
        // Force a refresh once on startup to pick up newly granted claims quickly.
        syncClaimsFromUser(auth.currentUser, forceRefresh = true)

        auth.addAuthStateListener { firebaseAuth ->
            syncClaimsFromUser(firebaseAuth.currentUser, forceRefresh = true)
        }
    }

    /**
     * Observes the isAdmin and isDeveloper claims reactively.
     * Uses cached tokens to minimize network overhead.
     */
    fun observeClaims(): Flow<UserClaims> = callbackFlow {
        // Initial check from current user state if available
        launch {
            val initialClaims = fetchClaims(forceRefresh = false)
            _isAdmin.value = initialClaims.isAdmin
            _isDeveloper.value = initialClaims.isDeveloper
            _isCommunityAdmin.value = initialClaims.isCommunityAdmin
            trySend(initialClaims)
        }

        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                _isAdmin.value = false
                _isDeveloper.value = false
                _isCommunityAdmin.value = false
                trySend(UserClaims())
            } else {
                // When auth state changes, check claims (cached) without blocking this callback.
                user.getIdToken(false)
                    .addOnSuccessListener { tokenResult ->
                        val isAdminClaim = parseClaimBoolean(tokenResult.claims, "isAdmin") ||
                            parseClaimBoolean(tokenResult.claims, "isSystemAdmin")
                        val isDevClaim = parseClaimBoolean(tokenResult.claims, "isDeveloper")
                        val isCommunityAdminClaim = parseClaimBoolean(tokenResult.claims, "isCommunityAdmin")
                        _isAdmin.value = isAdminClaim
                        _isDeveloper.value = isDevClaim
                        _isCommunityAdmin.value = isCommunityAdminClaim
                        trySend(UserClaims(isAdmin = isAdminClaim, isDeveloper = isDevClaim, isCommunityAdmin = isCommunityAdminClaim))
                    }
                    .addOnFailureListener { e ->
                        Logger.w(LogTags.AUTH, "Failed to fetch cached claims in auth listener; keeping last known claims: ${e.message}")
                        trySend(UserClaims(isAdmin = _isAdmin.value, isDeveloper = _isDeveloper.value, isCommunityAdmin = _isCommunityAdmin.value))
                    }
            }
        }
        
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    /**
     * Forces a refresh of the ID token to pick up new custom claims.
     */
    suspend fun refreshClaims(): Boolean {
        return try {
            val claims = fetchClaims(forceRefresh = true)
            _isAdmin.value = claims.isAdmin
            _isDeveloper.value = claims.isDeveloper
            _isCommunityAdmin.value = claims.isCommunityAdmin
            true
        } catch (e: Exception) {
            Logger.e(LogTags.AUTH, "Failed to refresh custom claims", e)
            false
        }
    }

    private suspend fun fetchClaims(forceRefresh: Boolean): UserClaims {
        val user = auth.currentUser ?: return UserClaims()
        val tokenResult = user.getIdToken(forceRefresh).await()
        val isAdmin = parseClaimBoolean(tokenResult.claims, "isAdmin") ||
            parseClaimBoolean(tokenResult.claims, "isSystemAdmin")
        val isDeveloper = parseClaimBoolean(tokenResult.claims, "isDeveloper")
        val isCommunityAdmin = parseClaimBoolean(tokenResult.claims, "isCommunityAdmin")
        return UserClaims(isAdmin = isAdmin, isDeveloper = isDeveloper, isCommunityAdmin = isCommunityAdmin)
    }

    private fun syncClaimsFromUser(user: com.google.firebase.auth.FirebaseUser?, forceRefresh: Boolean) {
        if (user == null) {
            _isAdmin.value = false
            _isDeveloper.value = false
            _isCommunityAdmin.value = false
            return
        }

        user.getIdToken(forceRefresh)
            .addOnSuccessListener { tokenResult ->
                _isAdmin.value = parseClaimBoolean(tokenResult.claims, "isAdmin") ||
                    parseClaimBoolean(tokenResult.claims, "isSystemAdmin")
                _isDeveloper.value = parseClaimBoolean(tokenResult.claims, "isDeveloper")
                _isCommunityAdmin.value = parseClaimBoolean(tokenResult.claims, "isCommunityAdmin")
            }
            .addOnFailureListener { e ->
                Logger.w(LogTags.AUTH, "Failed to sync claims from token; keeping last known claims: ${e.message}")
            }
    }

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
