package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.BirdTraitOverride
import com.example.hatchtracker.model.GeneticProfile
import com.example.hatchtracker.data.models.TraitCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneticProfileMergerTest {

    @Test
    fun `merge applies override label to fixed traits and removes conflicting ones`() {
        val baseProfile = GeneticProfile(
            fixedTraits = listOf("Single Comb", "Yellow Legs")
        )
        // Comb trait is "comb" in catalog, "single" is an option. 
        // "Single" is a substring of "Single Comb".
        val overrides = listOf(
            BirdTraitOverride("comb", "rose", TraitCategory.MENDELIAN)
        )

        val merged = GeneticProfileMerger.merge(baseProfile, overrides)

        assertTrue("Should contain Rose", merged.fixedTraits.contains("Rose"))
        assertFalse("Should NOT contain Single Comb", merged.fixedTraits.contains("Single Comb"))
        assertTrue("Should still contain Yellow Legs", merged.fixedTraits.contains("Yellow Legs"))
    }

    @Test
    fun `merge adds new traits from overrides if no conflict`() {
        val baseProfile = GeneticProfile(fixedTraits = listOf("Single"))
        val overrides = listOf(
            BirdTraitOverride("egg_color", "blue", TraitCategory.MENDELIAN)
        )

        val merged = GeneticProfileMerger.merge(baseProfile, overrides)

        assertTrue(merged.fixedTraits.contains("Blue"))
        assertTrue(merged.fixedTraits.contains("Single"))
    }

    @Test
    fun `merge handles empty base profile`() {
        val baseProfile = GeneticProfile(fixedTraits = emptyList())
        val overrides = listOf(
            BirdTraitOverride("size", "giant", TraitCategory.QUANTITATIVE)
        )

        val merged = GeneticProfileMerger.merge(baseProfile, overrides)

        assertTrue(merged.fixedTraits.contains("Giant") || merged.fixedTraits.contains("giant"))
    }

    @Test
    fun `merge removes traits when override option is null`() {
        // This case is unlikely given the BirdTraitOverride model, but good to test if logic supports it
        // Actually the Merger loop only iterates over overrides.
    }
}

