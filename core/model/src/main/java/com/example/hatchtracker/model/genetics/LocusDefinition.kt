package com.example.hatchtracker.model.genetics

data class LocusDefinition(
    val locusId: String,
    val displayName: String,
    val inheritance: InheritanceType = InheritanceType.AUTOSOMAL,
    val dominance: DominanceType = DominanceType.DOMINANT,
    val alleles: Set<String>,
    val defaultWildtype: String,
    
    // Genetics Phase 2: Quantitative/Qualitative Metadata
    val heritabilityScore: Double? = null,
    
    val notes: String? = null,
    val linkageGroupId: String? = null
)
