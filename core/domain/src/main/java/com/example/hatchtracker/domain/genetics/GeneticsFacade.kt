package com.example.hatchtracker.domain.genetics

import com.example.hatchtracker.domain.breeding.BreedingPredictionService
import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.Sex
import com.example.hatchtracker.model.genetics.PhenotypeResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Legacy API Wrapper for Genetics Logic.
 * Delegating all calls to BreedingPredictionService.
 */
@Singleton
class GeneticsFacade @Inject constructor(
    private val breedingPredictionService: BreedingPredictionService
) {

    fun predict(sire: Bird, dam: Bird): PhenotypeResult {
        val species = sire.species

        val result = breedingPredictionService.predictBreeding(
            species = species,
            sireProfile = sire.geneticProfile,
            damProfile = dam.geneticProfile
        )
        return result.phenotypeResult
    }

    fun predictMale(sire: Bird, dam: Bird): PhenotypeResult {
        return predictSex(sire, dam, Sex.MALE)
    }

    fun predictFemale(sire: Bird, dam: Bird): PhenotypeResult {
        return predictSex(sire, dam, Sex.FEMALE)
    }

    private fun predictSex(sire: Bird, dam: Bird, sex: Sex): PhenotypeResult {
        val species = sire.species

        val result = breedingPredictionService.predictBreeding(
            species = species,
            sireProfile = sire.geneticProfile,
            damProfile = dam.geneticProfile,
            offspringSex = sex
        )
        return result.phenotypeResult
    }
}

