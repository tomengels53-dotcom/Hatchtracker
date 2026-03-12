package com.example.hatchtracker.data.models

/**
 * Represents an inheritance type for a genetic locus.
 */
enum class InheritancePattern {
    AUTOSOMAL,
    SEX_LINKED, // Z-linked in poultry
    INCOMPLETE_DOMINANT,
    CODOMINANT
}

/**
 * Represents a specific allele.
 */
data class Allele(
    val symbol: String,
    val name: String,
    val isDominant: Boolean,
    val hierarchy: Int = 0 
)

/**
 * Represents a locus/gene configuration.
 */
data class LocusDefinition(
    val id: String,
    val name: String,
    val pattern: InheritancePattern,
    val alleles: List<Allele>,
    val defaultAlleleSymbol: String,
    val category: TraitCategory = TraitCategory.MENDELIAN
)

/**
 * Genotype at a specific locus, supporting partial certainty.
 */
data class GenotypeAtLocus(
    val locusId: String,
    val allele1: String,
    val allele2: String? = null, // Null for hemizygous (females in Z-linked)
    val probability: Float = 1.0f 
)

/**
 * Full genetic profile for simulation.
 */
data class GenotypeBlueprint(
    val loci: Map<String, GenotypeAtLocus>
)

/**
 * Resolved trait value.
 */
data class PhenotypeResult(
    val traitId: String,
    val valueId: String,
    val label: String,
    val probability: Float = 1.0f
)
