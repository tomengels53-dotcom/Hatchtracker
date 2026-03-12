package com.example.hatchtracker.domain.hatchy.routing.services

import com.example.hatchtracker.domain.hatchy.routing.PoultrySpecies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CoreKnowledgeRetrievalTest {

    @Test
    fun `should retrieve correct incubation days for species`() {
        val chickenRecord = PoultryCoreKnowledge.IncubationPeriods[PoultrySpecies.CHICKEN]
        assertNotNull(chickenRecord)
        assertEquals(21, chickenRecord?.totalDays)
        
        val duckRecord = PoultryCoreKnowledge.IncubationPeriods[PoultrySpecies.DUCK]
        assertNotNull(duckRecord)
        assertEquals(28, duckRecord?.totalDays)
    }

    @Test
    fun `should retrieve correct brooder temperature for week`() {
        val week1Temp = PoultryCoreKnowledge.BrooderTempsF[1]
        assertEquals(95.0, week1Temp)
        
        val week4Temp = PoultryCoreKnowledge.BrooderTempsF[4]
        assertEquals(80.0, week4Temp)
    }

    @Test
    fun `should retrieve correct genetics fact`() {
        val f2Fact = PoultryCoreKnowledge.GeneticsFacts["F2"]
        assertNotNull(f2Fact)
        assert(f2Fact!!.contains("Second generation"))
    }
}
