package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.BirdTraitOverride
import com.example.hatchtracker.data.models.Flock
import com.example.hatchtracker.model.GeneticProfile
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.data.models.TraitCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneticProfileResolverTest {

    private val resolver = GeneticProfileResolver()

    @Test
    fun `resolveGeneticProfile applies overrides for mixed birds`() {
        val bird = createMockBird(
            breedId = "Mixed",
            traits = listOf("Brown Egg"),
            overrides = listOf(
                BirdTraitOverride("egg_color", "blue", TraitCategory.MENDELIAN)
            )
        )

        val resolved = runBlocking { resolver.resolveGeneticProfile(bird, null) }

        assertTrue("Should contain blue", resolved.fixedTraits.any { it.equals("Blue", ignoreCase = true) })
        // Merger logic removes "Brown" if it finds egg_color conflict.
        assertTrue("Should NOT contain Brown", !resolved.fixedTraits.contains("Brown"))
    }

    @Test
    fun `resolveGeneticProfile applies overrides for purebred birds with flock source`() {
        val flock = Flock(
            localId = 1,
            syncId = "flock_1",
            name = "Test Flock",
            species = Species.CHICKEN,
            purpose = "breeding",
            defaultGeneticProfile = GeneticProfile(fixedTraits = listOf("Pea Comb"))
        )
        val bird = createMockBird(
            breedId = "Ameraucana", // Purebred
            overrides = listOf(
                BirdTraitOverride("comb", "single", TraitCategory.MENDELIAN)
            )
        )

        val resolved = runBlocking { resolver.resolveGeneticProfile(bird, flock) }

        assertTrue("Should contain single", resolved.fixedTraits.any { it.equals("Single", ignoreCase = true) })
        assertTrue("Should NOT contain Pea Comb", !resolved.fixedTraits.contains("Pea Comb"))
    }

    private fun createMockBird(
        breedId: String,
        traits: List<String> = emptyList(),
        overrides: List<BirdTraitOverride> = emptyList()
    ): Bird {
        return Bird(
            localId = 1,
            syncId = "bird_1",
            species = Species.CHICKEN,
            breed = breedId,
            breedId = breedId,
            hatchDate = "2023-01-01",
            geneticProfile = GeneticProfile(
                fixedTraits = traits,
                traitOverrides = overrides
            )
        )
    }
}

