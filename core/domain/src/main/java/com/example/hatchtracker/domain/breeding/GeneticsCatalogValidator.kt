package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.BreedStandard
import com.example.hatchtracker.model.genetics.BreedType
import com.example.hatchtracker.model.genetics.LocusDefinition
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeneticsCatalogValidator @Inject constructor() {
    
    /**
     * Validates breed objects for engine readiness.
     * Splits into blocking errors and advisory warnings.
     */
    fun validateBreeds(breeds: List<BreedStandard>): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val seenIds = mutableSetOf<String>()

        breeds.forEach { breed ->
            // BLOCKING ERRORS
            if (breed.id in seenIds) {
                errors.add("BLOCKING: Duplicate breed ID detected: ${breed.id}")
            }
            seenIds.add(breed.id)

            if (breed.breedType !in BreedType.entries) {
                errors.add("BLOCKING: Invalid breedType '${breed.breedType}' in breed ${breed.name}")
            }

            // NON-BLOCKING WARNINGS
            if (breed.geneticTags.size > 500) {
                warnings.add("WARNING: Genetic tags for ${breed.name} are unusually long. Verify data density.")
            }
            
            if (breed.crossbreedingNotes.isNullOrBlank() && breed.breedType == BreedType.HYBRID) {
                warnings.add("WARNING: Hybrid breed ${breed.name} is missing documented crossbreeding notes.")
            }
        }
        return ValidationResult(errors, warnings)
    }

    /**
     * Validates locus definitions for probabilistic logic.
     */
    fun validateLoci(loci: List<LocusDefinition>): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val seenLoci = mutableSetOf<String>()

        loci.forEach { locus ->
            if (locus.locusId in seenLoci) {
                errors.add("BLOCKING: Duplicate Locus identifier: ${locus.locusId}")
            }
            seenLoci.add(locus.locusId)

            locus.heritabilityScore?.let {
                if (it < 0.0 || it > 1.0) {
                    errors.add("BLOCKING: Heritability score $it out of range [0, 1] for locus ${locus.displayName}")
                }
                if (it > 0.8) {
                    warnings.add("WARNING: Unusually high heritability ($it) for locus ${locus.displayName}. Verify source.")
                }
            }
            
            if (locus.alleles.isEmpty()) {
                errors.add("BLOCKING: Locus ${locus.displayName} has no alleles defined.")
            }
        }
        return ValidationResult(errors, warnings)
    }

    data class ValidationResult(
        val errors: List<String>,
        val warnings: List<String>
    ) {
        val isValid: Boolean get() = errors.isEmpty()
    }
}
