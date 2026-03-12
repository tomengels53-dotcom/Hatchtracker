package com.example.hatchtracker.model.genetics

/**
 * Representation for calculation.
 * For AUTOSOMAL: alleles size == 2
 * For Z_LINKED:
 *  - Rooster ZZ: alleles size == 2
 *  - Hen ZW: alleles size == 1 (Z allele only), W is implicit
 */
data class GenotypeAtLocus(
    val locusId: String,
    val alleles: List<String>,
    val isAssumed: Boolean = true // Legacy flag, try to use certainty in GenotypeCall
)
