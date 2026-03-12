package com.example.hatchtracker.data.models

data class BreedingProgramAssetLink(
    val type: AssetType, 
    val refId: String,   // ALWAYS cloudId
    val role: LinkRole,  
    val generationIndex: Int?,
    val status: LinkStatus = LinkStatus.ACTIVE,
    val meta: Map<String, String> = emptyMap()
)

data class BreedingProgramAuditEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val actorId: String? = null
)

enum class AssetType { FLOCK, FLOCKLET, BIRD, INCUBATION }
enum class LinkRole { SIRE_SOURCE, DAM_SOURCE, BREEDER_POOL, INCUBATION }
enum class LinkStatus { ACTIVE, GRADUATED, ARCHIVED }
enum class MergeMode { KEEP_SEPARATE, MERGE }
