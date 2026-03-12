package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.auth.UserAuthManager
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.data.models.EvidenceType
import com.example.hatchtracker.data.models.TraitObservation
import com.example.hatchtracker.data.models.TraitVote
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Manages community-sourced trait validation logic.
 */
object CommunityValidationManager {
    private val db by lazy { FirebaseFirestore.getInstance() }

    /**
     * Submits a new trait observation.
     */
    suspend fun submitObservation(
        breedId: String,
        traitId: String,
        observedValue: String,
        confidence: Double,
        parentPairId: String,
        evidenceType: EvidenceType = EvidenceType.NONE,
        photoUrls: List<String> = emptyList(),
        notes: String? = null
    ): Result<String> {
        val user = UserAuthManager.currentUser.value ?: return Result.failure(Exception("User not logged in"))
        
        // Hashing user ID for partial anonymity in public records (simple hash for demo)
        val publicUserId = user.uid.hashCode().toString() 

        val observationRef = db.collection("traitObservations").document()
        val observation = TraitObservation(
            id = observationRef.id,
            breedId = breedId,
            traitId = traitId,
            observedValue = observedValue,
            confidence = confidence,
            parentPairId = parentPairId,
            environmentalNotes = notes,
            evidenceType = evidenceType,
            photoUrls = photoUrls,
            userId = user.uid, // Stored fully for admin/system, UI can use hashed if needed
            timestamp = System.currentTimeMillis()
        )

        return try {
            observationRef.set(observation).await()
            Result.success(observationRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Casts a vote on an observation.
     */
    suspend fun voteOnObservation(
        observationId: String,
        agree: Boolean
    ): Result<Unit> {
        val user = UserAuthManager.currentUser.value ?: return Result.failure(Exception("Not logged in"))
        val userProfile: com.example.hatchtracker.domain.model.UserProfile? = null // TODO: Refactor Manager to use Injection
        
        // Check if user is voting on their own submission
        val obsSnapshot = db.collection("traitObservations").document(observationId).get().await()
        val observation = obsSnapshot.toObject(TraitObservation::class.java)
        if (observation == null) return Result.failure(Exception("Observation not found"))

        if (observation.userId == user.uid) {
            return Result.failure(Exception("Cannot vote on your own submission"))
        }

        // Calculate Vote Power based on Reputation
        val basePower = 1
        val reputationBonus = ((userProfile?.reputation ?: 0) / 100).coerceAtLeast(0)
        val votePower = basePower + reputationBonus

        val vote = TraitVote(
            observationId = observationId,
            userId = user.uid,
            voteTokens = votePower,
            agree = agree,
            timestamp = System.currentTimeMillis()
        )

        return try {
            // Save vote
            db.collection("traitVotes")
                .document("${observationId}_${user.uid}") // Unique ID per user-observation
                .set(vote)
                .await()
            
            // Trigger recalculation (in real app, this might be a Cloud Function)
            // For now, we perform a client-side update of the aggregate (optimistic)
            updateConfidenceScore(observation.breedId, observation.traitId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recalculates confidence score for a trait (Stub for Cloud Function logic).
     */
    private suspend fun updateConfidenceScore(breedId: String, traitId: String) {
        // Fetch all observations and votes for this trait...
        // This is heavy for client-side, usually done via Cloud Functions.
        // We will log this action for now.
        Logger.d(LogTags.BREEDING, "Triggering confidence update for $breedId / $traitId")
    }
}
