package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.domain.genetics.GeneticLocusCatalog
import com.example.hatchtracker.model.Species
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull

class GeneticLocusCatalogCoverageTest {

    @Test
    fun `all species should have defined loci`() {
        Species.values().forEach { species ->
            if (species != Species.UNKNOWN) {
                val loci = GeneticLocusCatalog.lociForSpecies(species)
                assertNotNull("Loci list should not be null for ${species.name}", loci)
                // We expect at least some loci for main species, though minor ones might be empty initially
                if (species == Species.CHICKEN || species == Species.DUCK) {
                    assertTrue("Expected loci for ${species.name}", loci.isNotEmpty())
                }
            }
        }
    }

    @Test
    fun `breeding goal catalog should return goals for main species`() {
        val catalog = BreedingGoalTraitCatalog()
        val goals = catalog.getAvailableGoals(Species.CHICKEN)
        assertTrue("Chicken should have breeding goals", goals.isNotEmpty())
        
        val hasQuant = goals.any { it.type == GoalOptionType.QUANTITATIVE_RANGE }
        assertTrue("Chicken should have quantitative goals (e.g. Temperament)", hasQuant)
    }
}

