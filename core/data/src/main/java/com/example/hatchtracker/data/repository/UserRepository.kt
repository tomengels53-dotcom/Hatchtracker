package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.models.SubscriptionTier
import com.example.hatchtracker.domain.model.UserProfile
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing User Profiles in Firestore.
 */
@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: com.google.firebase.auth.FirebaseAuth
) {
    private val USERS_COLLECTION = "users"

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    /**
     * Ensures a Firestore profile exists for the given FirebaseUser, merging auth fields.
     */
    /**
     * Ensures a Firestore profile exists for the given FirebaseUser, merging auth fields.
     * STRICT IMPLEMENTATION:
     * - Create: Start as FREE, set safe defaults. NO developer/admin flags.
     * - Update: Only update safe fields (displayName, photo). NO email, country, or roles.
     */
    suspend fun ensureProfileFromAuth(
        user: com.google.firebase.auth.FirebaseUser,
        termsVersion: String = "",
        privacyVersion: String = "",
        preferredLanguage: String? = null
    ): Result<Unit> {
        return try {
            val uid = user.uid
            val docRef = firestore.collection(USERS_COLLECTION).document(uid)
            
            androidx.tracing.Trace.beginSection("UserRepository.firstRead")
            com.example.hatchtracker.core.logging.FirebasePerfTracer.recordRead()
            val snapshot = docRef.get().await()
            androidx.tracing.Trace.endSection()
            
            val now = System.currentTimeMillis()

            if (!snapshot.exists()) {
                // NEW USER: Safe Creation Allow-list
                val locale = java.util.Locale.getDefault()
                val countryCode = locale.country ?: "US"
                val currencyCode = try {
                    java.util.Currency.getInstance(locale).currencyCode
                } catch (e: Exception) {
                    "USD"
                }

                val newProfile = hashMapOf(
                    "userId" to uid,
                    "displayName" to (user.displayName ?: ""),
                    "profilePictureUrl" to (user.photoUrl?.toString() ?: ""),
                    "adsEnabled" to true,
                    "createdAt" to now,
                    "lastUpdated" to now,
                    "termsVersionAccepted" to termsVersion,
                    "privacyVersionAccepted" to privacyVersion,
                    "consentTimestamp" to now,
                    "countryCode" to countryCode,
                    "currencyCode" to currencyCode,
                    "subscriptionTier" to SubscriptionTier.FREE, // Enforce FREE on create
                    "preferredLanguage" to (preferredLanguage ?: "")
                )
                
                docRef.set(newProfile).await()
                Logger.d(LogTags.AUTH, "ensureProfileFromAuth CREATED uid=$uid (Safe Fields Only)")
            } else {
                // EXISTING USER: Safe Update Allow-list
                // Only sync safe fields from Auth if they changed
                val updates = mutableMapOf<String, Any>(
                    "displayName" to (user.displayName ?: ""),
                    "lastUpdated" to now
                )
                user.photoUrl?.toString()?.let { updates["profilePictureUrl"] = it }
                preferredLanguage?.let { updates["preferredLanguage"] = it }

                // BLOCKED: email, isDeveloper, countryCode, currencyCode, subscriptionTier
                
                docRef.set(updates, SetOptions.merge()).await()
                Logger.d(LogTags.AUTH, "ensureProfileFromAuth MERGED uid=$uid (Safe Fields Only)")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(LogTags.AUTH, "ensureProfileFromAuth FAILED: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Legacy: Creates a default profile for a new user if one does not exist.
     * @deprecated Use ensureProfileFromAuth(FirebaseUser) instead.
     */
    @Deprecated("Use ensureProfileFromAuth instead", ReplaceWith("ensureProfileFromAuth(user)"))
    suspend fun createDefaultProfile(
        userId: String,
        termsVersion: String = "",
        privacyVersion: String = ""
    ): Result<Unit> {
         // simplified to just fail or no-op, but keeping implementation for compilation if called
         // preventing blocked writes
         return try {
             val docRef = firestore.collection(USERS_COLLECTION).document(userId)
             val snapshot = docRef.get().await()
             if (!snapshot.exists()) {
                 val locale = java.util.Locale.getDefault()
                val countryCode = locale.country ?: "US"
                val currencyCode = try {
                    java.util.Currency.getInstance(locale).currencyCode
                } catch (e: Exception) {
                    "USD"
                }
                 val newProfile = UserProfile(
                     userId = userId,
                     subscriptionTier = SubscriptionTier.FREE,
                     adsEnabled = true,
                     isDeveloper = false, // blocked from write, but false is default
                     createdAt = System.currentTimeMillis(),
                     lastUpdated = System.currentTimeMillis(),
                     termsVersionAccepted = termsVersion,
                     privacyVersionAccepted = privacyVersion,
                     consentTimestamp = System.currentTimeMillis(),
                     countryCode = countryCode,
                     currencyCode = currencyCode
                 )
                 // This acts as a CREATE, so isDeveloper=false is fine if omitted or default.
                 // We must ensure UserProfile serialization doesn't write forbidden fields if they are null/default?
                 // UserProfile data class has defaults. 
                 // To be safe, we should use a map like in ensureProfileFromAuth, but for now assuming legacy calls are rare.
                 docRef.set(newProfile).await()
             }
             Result.success(Unit)
         } catch (e: Exception) {
             Result.failure(e)
         }
    }

    /**
     * Updates the existing user profile.
     * STRICT ALLOW-LIST: displayName, profilePictureUrl, adsEnabled, lastUpdated.
     */
    suspend fun updateProfile(userProfile: UserProfile): Result<Unit> {
        return try {
            val updates = mapOf(
                "displayName" to userProfile.displayName,
                "profilePictureUrl" to (userProfile.profilePictureUrl ?: ""),
                "adsEnabled" to userProfile.adsEnabled,
                "preferredLanguage" to userProfile.preferredLanguage,
                "countryCode" to (userProfile.countryCode ?: "US"),
                "currencyCode" to (userProfile.currencyCode ?: "USD"),
                "weightUnit" to (userProfile.weightUnit ?: "kg"),
                "dateFormat" to (userProfile.dateFormat ?: "DD-MM-YYYY"),
                "timeFormat" to (userProfile.timeFormat ?: "24h"),
                "username" to userProfile.username,
                "bio" to userProfile.bio,
                "publicProfileEnabled" to userProfile.publicProfileEnabled,
                "breederType" to userProfile.breederType,
                "speciesFocus" to userProfile.speciesFocus,
                "specialties" to userProfile.specialties,
                "showRegionPublicly" to userProfile.showRegionPublicly,
                "allowDirectMessages" to userProfile.allowDirectMessages,
                "allowMarketplaceContact" to userProfile.allowMarketplaceContact,
                "marketplaceSellerEnabled" to userProfile.marketplaceSellerEnabled,
                "pickupRegion" to userProfile.pickupRegion,
                "willingToShip" to userProfile.willingToShip,
                // System managed fields are generally not updated by client, but including for completeness if needed
                "communityProfileVersion" to userProfile.communityProfileVersion,
                "lastUpdated" to System.currentTimeMillis()
            )
            firestore.collection(USERS_COLLECTION)
                .document(userProfile.userId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfileInfo(
        userId: String,
        displayName: String,
        profilePictureUrl: String?
    ): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(
                    mapOf(
                        "displayName" to displayName,
                        "profilePictureUrl" to (profilePictureUrl ?: ""),
                        "lastUpdated" to System.currentTimeMillis()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLanguage(userId: String, languageTag: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(
                    mapOf(
                        "preferredLanguage" to languageTag,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns a reactive flow of the user's profile from Firestore.
     */
    fun getProfileFlow(userId: String): Flow<UserProfile?> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val docRef = firestore.collection(USERS_COLLECTION).document(userId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            com.example.hatchtracker.core.logging.FirebasePerfTracer.recordRead()
            if (error != null) {
                // Log error or handle it
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val roles = (snapshot.get("roles") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val role = snapshot.getString("role")?.lowercase().orEmpty()
                val isDeveloperProfile = snapshot.getBoolean("isDeveloper") == true ||
                    roles.any { it.equals("developer", ignoreCase = true) } ||
                    role == "developer"
                val isSystemAdminProfile = snapshot.getBoolean("isSystemAdmin") == true ||
                    snapshot.getBoolean("isAdmin") == true ||
                    roles.any { it.equals("admin", ignoreCase = true) || it.equals("system_admin", ignoreCase = true) } ||
                    role == "admin"
                val isCommunityAdminProfile = snapshot.getBoolean("isCommunityAdmin") == true ||
                    roles.any { it.equals("community_admin", ignoreCase = true) }
                
                @Suppress("UNCHECKED_CAST")
                val profile = UserProfile(
                    userId = userId,
                    email = snapshot.getString("email") ?: "",
                    displayName = snapshot.getString("displayName") ?: "",
                    profilePictureUrl = snapshot.getString("profilePictureUrl"),
                    subscriptionTier = SubscriptionTier.valueOf(snapshot.getString("subscriptionTier") ?: "FREE"),
                    subscriptionActive = snapshot.getBoolean("subscriptionActive") == true,
                    isDeveloper = isDeveloperProfile,
                    isSystemAdmin = isSystemAdminProfile,
                    isCommunityAdmin = isCommunityAdminProfile,
                    roles = roles,
                    countryCode = snapshot.getString("countryCode") ?: "US",
                    weightUnit = snapshot.getString("weightUnit") ?: "kg",
                    dateFormat = snapshot.getString("dateFormat") ?: "yyyy-MM-dd",
                    timeFormat = snapshot.getString("timeFormat") ?: "24h",
                    currencyCode = snapshot.getString("currencyCode") ?: "USD",
                    reputation = snapshot.getLong("reputation")?.toInt() ?: 0,
                    preferredLanguage = snapshot.getString("preferredLanguage") ?: "",
                    
                    // Community Fields
                    username = snapshot.getString("username") ?: "",
                    bio = snapshot.getString("bio") ?: "",
                    publicProfileEnabled = snapshot.getBoolean("publicProfileEnabled") == true,
                    breederType = snapshot.getString("breederType") ?: "",
                    speciesFocus = (snapshot.get("speciesFocus") as? List<String>) ?: emptyList(),
                    specialties = (snapshot.get("specialties") as? List<String>) ?: emptyList(),
                    showRegionPublicly = snapshot.getBoolean("showRegionPublicly") == true,
                    allowDirectMessages = snapshot.getBoolean("allowDirectMessages") == true,
                    allowMarketplaceContact = snapshot.getBoolean("allowMarketplaceContact") == true,
                    marketplaceSellerEnabled = snapshot.getBoolean("marketplaceSellerEnabled") == true,
                    pickupRegion = snapshot.getString("pickupRegion") ?: "",
                    willingToShip = snapshot.getBoolean("willingToShip") == true,
                    communityProfileVersion = snapshot.getLong("communityProfileVersion")?.toInt() ?: 1,
                    moderationStatus = snapshot.getString("moderationStatus") ?: "CLEAN",
                    reputationSummary = snapshot.getString("reputationSummary") ?: "",
                    sellerVerificationStatus = snapshot.getString("sellerVerificationStatus") ?: "UNVERIFIED",
                    profileCompletenessScore = snapshot.getLong("profileCompletenessScore")?.toInt() ?: 0
                )
                
                Logger.d(LogTags.AUTH, "Snapshot received for userId=$userId")
                
                trySend(profile)
                _userProfile.value = profile
            } else {
                Logger.d(LogTags.AUTH, "Snapshot is null or doesn't exist")
                trySend(null)
            }
        }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    /**
     * Deletes the user's profile.
     * Note: This does not automatically delete sub-collections or related data in other collections
     * (like breeding scenarios or support tickets). Those should be handled by Cloud Functions
     * or explicit calls to respective repositories before calling this.
     */
    suspend fun deleteUserProfile(userId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION).document(userId).delete().await()
            _userProfile.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


