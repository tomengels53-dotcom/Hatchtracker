package com.example.hatchtracker.domain.breeding.starter

import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.Sex
import com.example.hatchtracker.model.GeneticProfile
import com.example.hatchtracker.data.models.ConfidenceLevel

object VirtualBirdFactory {

    fun createFromTemplate(template: ReferenceStarterTemplate, sex: Sex): Bird {
        return Bird(
            localId = -(System.currentTimeMillis() % 1000000) - (Math.random() * 1000).toLong(),
            syncId = "starter_virtual_${template.id}_${sex.name}",
            species = template.species,
            breed = template.title,
            sex = sex,
            hatchDate = "TBD",
            geneticProfile = GeneticProfile(
                fixedTraits = template.fixedTraits,
                genotypeCalls = template.genotypePriors,
                confidenceLevel = ConfidenceLevel.HIGH.name
            )
        )
    }
}

