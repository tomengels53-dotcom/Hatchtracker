package com.example.hatchtracker.domain.genetics.engine

import com.example.hatchtracker.model.genetics.*
import com.example.hatchtracker.domain.genetics.GeneticLocusCatalog
import com.example.hatchtracker.model.Sex
import com.example.hatchtracker.model.Species
import org.junit.Assert.assertEquals
import org.junit.Test

class GeneticProbabilityEngineTest {

    @Test
    fun `test autosomal dominance inheritance (Naked Neck)`() {
        // Heterozygous Sire (Na/na+) x Wildtype Dam (na+/na+)
        val naCall = GenotypeCall(GeneticLocusCatalog.LOCUS_NA, listOf("Na", "na+"), Certainty.CONFIRMED)
        val wtCall = GenotypeCall(GeneticLocusCatalog.LOCUS_NA, listOf("na+", "na+"), Certainty.CONFIRMED)
        
        val result = GeneticProbabilityEngine.predict(
            species = Species.CHICKEN,
            mapOf(GeneticLocusCatalog.LOCUS_NA to naCall),
            mapOf(GeneticLocusCatalog.LOCUS_NA to wtCall)
        )
        
        val dist = result[GeneticLocusCatalog.LOCUS_NA]!!
        val pNa = dist.probabilityOfAllele("Na")
        
        // Punnett:
        // Na, na+
        // na+, na+
        // Outcomes: Na/na+, Na/na+, na+/na+, na+/na+ -> 50% Na
        assertEquals(0.5, pNa, 0.01)
    }

    @Test
    fun `test incomplete dominance (Blue)`() {
        // Blue (Bl/bl+) x Blue (Bl/bl+) -> 25% Black, 50% Blue, 25% Splash
        val blueCall = GenotypeCall(GeneticLocusCatalog.LOCUS_BL, listOf("Bl", "bl+"), Certainty.CONFIRMED)
        
        val result = GeneticProbabilityEngine.predict(
            species = Species.CHICKEN,
            maleGenotype = mapOf(GeneticLocusCatalog.LOCUS_BL to blueCall),
            femaleGenotype = mapOf(GeneticLocusCatalog.LOCUS_BL to blueCall)
        )
        
        val dist = result[GeneticLocusCatalog.LOCUS_BL]!!
        val pSplash = dist.probabilityHomozygous("Bl") // Bl/Bl
        
        // P(Bl/Bl) = 0.25
        assertEquals(0.25, pSplash, 0.01)
    }
    
    @Test
    fun `test Z-linked inheritance Black Sex-Link (Red Sire x Barred Dam)`() {
        // Sire: Gold (bb) - "Red" is recessive wildtype b+/b+ or specific?
        // Wait, B Locus: B = Barring, b+ = Non-barring/Gold base usually allows barring to show or not but locus is B.
        // Barred Rock (Hen) = Z(B) W
        // RIR (Rooster) = Z(b+) Z(b+)
        
        val sire = GenotypeCall(GeneticLocusCatalog.LOCUS_B, listOf("b+", "b+"), Certainty.CONFIRMED)
        val dam = GenotypeCall(GeneticLocusCatalog.LOCUS_B, listOf("B"), Certainty.CONFIRMED) // Hen has 1 allele
        
        // Predict Males (ZZ)
        val malePred = GeneticProbabilityEngine.predict(
            species = Species.CHICKEN,
            maleGenotype = mapOf(GeneticLocusCatalog.LOCUS_B to sire),
            femaleGenotype = mapOf(GeneticLocusCatalog.LOCUS_B to dam),
            offspringSex = Sex.MALE
        )
        // Males get Z(b+) from Sire and Z(B) from Dam -> Z(B)Z(b+) -> Heterozygous Barred
        // Phenotype should be Barred.
        val maleDist = malePred[GeneticLocusCatalog.LOCUS_B]!!
        assertEquals(1.0, maleDist.probabilityOfAllele("B"), 0.01) 
        
        // Predict Females (ZW)
        val femalePred = GeneticProbabilityEngine.predict(
            species = Species.CHICKEN,
            maleGenotype = mapOf(GeneticLocusCatalog.LOCUS_B to sire),
            femaleGenotype = mapOf(GeneticLocusCatalog.LOCUS_B to dam),
            offspringSex = Sex.FEMALE
        )
        // Females get Z(b+) from Sire and W from Dam -> Z(b+)W -> Non-Barred
        val femaleDist = femalePred[GeneticLocusCatalog.LOCUS_B]!!
        assertEquals(0.0, femaleDist.probabilityOfAllele("B"), 0.01)
        
        // Validates "Black Sex-Link" logic: Males Barred, Females Non-Barred
    }
}

