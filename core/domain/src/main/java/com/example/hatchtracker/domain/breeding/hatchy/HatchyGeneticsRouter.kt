package com.example.hatchtracker.domain.breeding.hatchy

import com.example.hatchtracker.domain.breeding.BreedingPredictionService
import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.Sex

/**
 * Routes genetics-specific questions from Hatchy (AI Assistant) to the deterministic engine.
 * PRO FEATURE ONLY.
 */
object HatchyGeneticsRouter {

    fun processQuery(
        sire: Bird, 
        dam: Bird, 
        isProUser: Boolean
    ): String {
        if (!isProUser) {
            val sireLabel = displayName(sire)
            val damLabel = displayName(dam)
            return "Detailed genetic analysis is a HatchBase PRO feature. Upgrade to see what $sireLabel and $damLabel will produce!"
        }

        val species = sire.species
        val service = BreedingPredictionService()
        val sireProfile = sire.geneticProfile
        val damProfile = dam.geneticProfile

        val malePred = service.predictBreeding(species, sireProfile, damProfile, Sex.MALE).phenotypeResult
        val femalePred = service.predictBreeding(species, sireProfile, damProfile, Sex.FEMALE).phenotypeResult
        val generalPred = service.predictBreeding(species, sireProfile, damProfile).phenotypeResult

        // 2. Build Explanation
        return GeneticsExplanationBuilder.buildExplanation(sire, dam, malePred, femalePred, generalPred)
    }

    private fun displayName(bird: Bird): String {
        return bird.breed.takeIf { it.isNotBlank() }
            ?: "Bird ${bird.localId}"
    }
}

