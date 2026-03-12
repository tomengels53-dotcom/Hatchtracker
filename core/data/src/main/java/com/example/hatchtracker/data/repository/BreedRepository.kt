package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.models.Breed
import com.example.hatchtracker.data.models.BreedStandard
import com.example.hatchtracker.model.GeneticProfile
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching breed data from Firestore.
 */
@Singleton
class BreedRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /**
     * Fetches breeds for a given species ID (e.g., "chicken", "duck").
     * Maps Firestore "BreedStandard" schema to local "Breed" entity.
     */
    fun getBreedsForSpecies(speciesId: String): Flow<List<Breed>> = callbackFlow {
        if (speciesId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // Firestore species field is Capitalized (e.g., "Chicken"), but internal IDs are lowercase ("chicken")
        // We ensure the input is normalized to lowercase first in case it comes from Species enum .name (which is uppercase)
        val normalizedSpeciesId = speciesId.lowercase(Locale.getDefault())
        val firestoreSpecies = normalizedSpeciesId.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        
        Logger.d(LogTags.DB, "Querying for species: '$firestoreSpecies' (from ID: '$speciesId')")

        val query = firestore.collection(COLLECTION_PATH)
            .whereEqualTo("species", firestoreSpecies)
            // .orderBy("name") // Requires index, safer to sort client-side for dynamic queries initially

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Logger.e(LogTags.DB, "Error fetching breeds", error)
                // In a real app, emit error state
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                launch(Dispatchers.IO) {
                    Logger.d(LogTags.DB, "Found ${snapshot.documents.size} breed documents")
                    val breeds = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val name = doc.getString("name") ?: "Unknown"
                            val eggColor = doc.getString("eggColor") ?: "Unknown"
                            val weightHen = doc.getDouble("weightHenKg") ?: 0.0

                            val eggProductionCount = doc.getDouble("eggProduction")?.toInt()
                            val eggProductionLabel = when (eggProductionCount) {
                                null -> "Unknown"
                                in 241..Int.MAX_VALUE -> "Prolific"
                                in 150..240 -> "Normal"
                                else -> "Not Effective"
                            }

                            val size = doc.getString("size") ?: "Unknown"
                            val weightClass = doc.getString("weightClass") ?: "Unknown"

                            // Map to internal Breed model
                            Breed(
                                id = id,
                                speciesId = speciesId,
                                name = name,
                                eggColor = eggColor,
                                size = size,
                                weightClass = weightClass,
                                weightKg = weightHen,
                                imageResId = null,
                                climateSuitability = "Unknown",
                                eggProduction = eggProductionLabel,
                                weightLbs = weightHen * 2.20462,
                                heightInch = 0.0,
                                heightCm = 0.0,
                                dataSource = "CLOUD"
                            )
                        } catch (e: Exception) {
                            Logger.e(LogTags.DB, "Error parsing breed: ${doc.id}", e)
                            null
                        }
                    }.sortedBy { it.name }
                    
                    trySend(breeds)
                }
            } else {
                trySend(emptyList())
            }
        }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()
    /**
     * Fetches all breed standards from Firestore.
     */
    fun getAllBreedStandards(): Flow<List<BreedStandard>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_PATH)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Logger.e(LogTags.DB, "Error fetching all breed standards", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    launch(Dispatchers.IO) {
                        val breedStandards = snapshot.documents.mapNotNull { doc ->
                            doc.toBreedStandardSafe()
                        }
                        trySend(breedStandards)
                    }
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    /**
     * Fetches a specific breed standard by species and name.
     */
    fun getBreedStandard(species: String, breedName: String): Flow<BreedStandard?> = callbackFlow {
        if (species.isBlank() || breedName.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val query = firestore.collection(COLLECTION_PATH)
            .whereEqualTo("species", species)
            .whereEqualTo("name", breedName)
            .limit(1)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Logger.e(LogTags.DB, "Error fetching breed standard $breedName", error)
                trySend(null)
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                launch(Dispatchers.IO) {
                    val doc = snapshot.documents.first()
                    trySend(doc.toBreedStandardSafe())
                }
            } else {
                trySend(null)
            }
        }
        
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    private fun DocumentSnapshot.toBreedStandardSafe(): BreedStandard? {
        if (!exists()) return null
        return try {
            // Manual handling for the polymorph lastUpdated field
            val rawLu = get("lastUpdated")
            val lu = when (rawLu) {
                is Long -> rawLu
                is Timestamp -> rawLu.toDate().time
                is Number -> rawLu.toLong()
                else -> null
            }

            // We use toObject for major mapping but explicitly set the tricky fields
            // If the whole toObject fails, we fallback to a minimal manual map
            try {
                this.toObject(BreedStandard::class.java)?.copy(id = id, lastUpdated = lu)
            } catch (e: Exception) {
                Logger.w(LogTags.DB, "toObject failed for BreedStandard $id, falling back to manual mapping")
                BreedStandard(
                    id = id,
                    name = getString("name") ?: "Unknown",
                    species = getString("species") ?: "Chicken",
                    eggColor = getString("eggColor") ?: "Unknown",
                    lastUpdated = lu,
                    geneticProfile = get("geneticProfile")?.let {
                        try {
                            // Try to map nested genetic profile if possible
                            (it as? Map<*, *>)?.let { map ->
                                GeneticProfile(
                                    confidenceLevel = (map["confidenceLevel"] as? String) ?: "LOW"
                                )
                            }
                        } catch (e: Exception) { null }
                    } ?: GeneticProfile()
                )
            }
        } catch (e: Exception) {
            Logger.e(LogTags.DB, "Critical failure mapping BreedStandard $id", e)
            null
        }
    }

    private companion object {
        private const val COLLECTION_PATH = "breedStandards"
    }
}

