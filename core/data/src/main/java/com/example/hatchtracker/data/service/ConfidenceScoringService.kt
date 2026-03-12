package com.example.hatchtracker.data.service

import com.example.hatchtracker.data.models.ConfidenceEvent
import com.example.hatchtracker.data.models.ConfidenceLevel
import com.example.hatchtracker.data.models.TraitConfidenceState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Service responsible for calculating and updating trait confidence scores.
 * Note: In a production environment, this logic should primarily reside in Cloud Functions.
 */
object ConfidenceScoringService {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private const val COLLECTION_NAME = "traitConfidence"

    /**
     * Updates the confidence state for a trait based on new evidence.
     */
    suspend fun processEvidence(
        breedId: String,
        traitId: String,
        delta: Double,
        reason: String
    ) {
        val docId = "${breedId}_${traitId}"
        val docRef = db.collection(COLLECTION_NAME).document(docId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentState = if (snapshot.exists()) {
                snapshot.toObject(TraitConfidenceState::class.java)!!
            } else {
                TraitConfidenceState(
                    id = docId,
                    breedId = breedId,
                    traitId = traitId
                )
            }

            // 1. Calculate new Score
            // Clamp between 0.0 and 100.0
            val newScore = (currentState.confidenceScore + delta).coerceIn(0.0, 100.0)

            // 2. Determine new Level
            val newLevel = when {
                newScore >= 90.0 -> {
                    // Only upgrade to FIXED if it was already FIXED or via specific admin tool.
                    // Ideally, we don't auto-promote to FIXED.
                    if (currentState.confidenceLevel == ConfidenceLevel.FIXED) ConfidenceLevel.FIXED else ConfidenceLevel.HIGH
                }
                newScore >= 75.0 -> ConfidenceLevel.HIGH
                newScore >= 50.0 -> ConfidenceLevel.MEDIUM
                else -> ConfidenceLevel.LOW
            }

            // 3. Update Evidence Counters (Simplified logic)
            val newSupporting = if (delta > 0) currentState.supportingObservations + 1 else currentState.supportingObservations
            val newConflicting = if (delta < 0) currentState.conflictingObservations + 1 else currentState.conflictingObservations

            // 4. Create Event
            val event = ConfidenceEvent(
                timestamp = System.currentTimeMillis(),
                delta = delta,
                reason = reason
            )

            // 5. Build New State
            val newState = currentState.copy(
                confidenceScore = newScore,
                confidenceLevel = newLevel,
                supportingObservations = newSupporting,
                conflictingObservations = newConflicting,
                lastUpdated = System.currentTimeMillis(),
                events = currentState.events + event
            )

            transaction.set(docRef, newState)
        }.await()
    }

    /**
     * Calculates the probability multiplier based on confidence level.
     * Used by BreedingRecommender.
     */
    fun getConfidenceMultiplier(level: ConfidenceLevel): Double {
        return when (level) {
            ConfidenceLevel.LOW -> 0.5   // Penalize low confidence heavily
            ConfidenceLevel.MEDIUM -> 0.8 // Slight penalty
            ConfidenceLevel.HIGH -> 1.0   // Trust fully
            ConfidenceLevel.FIXED -> 1.0  // Trust fully
        }
    }
}
