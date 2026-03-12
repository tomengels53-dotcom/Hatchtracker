package com.example.hatchtracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class SimulationTraitResult(
    val traitId: String,
    val trait: String,
    val inheritedFrom: String,
    val probability: Double,
    val confidence: String,
    val parentTraitRefs: List<String> = emptyList()
)

data class SimulationGenerationResult(
    val generationNumber: Int,
    val predictedTraits: List<SimulationTraitResult>,
    val confidenceScore: Double = 0.0,
    val diversityIndex: Double = 0.0,
    val inbreedingRiskLevel: String = "UNKNOWN"
)

/**
 * Repository for managing Breeding Simulation data in Firestore.
 */
class SimulationRepository {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Persists simulation results to Firestore.
     *
     * Structure:
     * breedingSimulations/{simulationId}
     * breedingSimulations/{simulationId}/generations/{n}
     */
    suspend fun saveSimulation(
        userId: String,
        parentAId: Long,
        parentBId: Long,
        results: List<SimulationGenerationResult>
    ): String {
        val simulationsRef = db.collection("breedingSimulations")

        // 1. Create Main Simulation Document
        val simulationData = mapOf(
            "ownerId" to userId,
            "parentAId" to parentAId,
            "parentBId" to parentBId,
            "generationCount" to results.size,
            "finalConfidence" to (results.lastOrNull()?.confidenceScore ?: 0.0),
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        com.example.hatchtracker.core.logging.FirebasePerfTracer.recordWrite()
        val docRef = simulationsRef.add(simulationData).await()
        val simulationId = docRef.id

        // 2. Save Individual Generations
        val generationsColl = docRef.collection("generations")

        results.forEach { gen ->
            val genData = mapOf(
                "number" to gen.generationNumber,
                "traits" to gen.predictedTraits.map {
                    mapOf(
                        "traitId" to it.traitId,
                        "trait" to it.trait,
                        "inheritedFrom" to it.inheritedFrom,
                        "probability" to it.probability,
                        "confidence" to it.confidence,
                        "parentTraitRefs" to it.parentTraitRefs
                    )
                },
                "metrics" to mapOf(
                    "confidence" to gen.confidenceScore,
                    "diversity" to gen.diversityIndex,
                    "inbreedingRisk" to gen.inbreedingRiskLevel
                )
            )
            com.example.hatchtracker.core.logging.FirebasePerfTracer.recordWrite()
            generationsColl.document(gen.generationNumber.toString()).set(genData).await()
        }

        return simulationId
    }

    /**
     * Retrieves simulation history for a specific user.
     */
    suspend fun getUserSimulations(userId: String): com.google.firebase.firestore.QuerySnapshot {
        com.example.hatchtracker.core.logging.FirebasePerfTracer.recordRead()
        return db.collection("breedingSimulations")
            .whereEqualTo("ownerId", userId)
            .get()
            .await()
    }
}
