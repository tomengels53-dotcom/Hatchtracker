package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.domain.genetics.GeneticLocusCatalog
import com.example.hatchtracker.model.Species
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalTargetConsistencyTest {

    private val catalog = GoalTemplateCatalog()

    @Test
    fun `no arbitrary string goal keys remain in templates`() {
        val templates = catalog.getTemplatesForSpecies(Species.CHICKEN)
        val knownLoci = GeneticLocusCatalog.lociForSpecies(Species.CHICKEN).map { it.locusId }

        templates.forEach { template ->
            template.mustHave.forEach { target ->
                assertTrue(
                    "Trait property '${target.traitId}' must be a recognized Locus ID, not an arbitrary string.",
                    knownLoci.contains(target.traitId)
                )
            }
        }
    }

    @Test
    fun `egg color goal uses O_Locus and allele O`() {
        val templates = catalog.getTemplatesForSpecies(Species.CHICKEN)
        val coloredEggTemplate = templates.find { it.id == "colored_eggs" }
        
        assertTrue("Template colored_eggs should exist", coloredEggTemplate != null)
        assertTrue(
            "colored_eggs should target O_Locus and O allele",
            coloredEggTemplate!!.mustHave.any { it.traitId == "O_Locus" && it.valueId == "O" }
        )
    }
}
