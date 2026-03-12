package com.example.hatchtracker.domain.hatchy

import com.example.hatchtracker.model.*
import com.example.hatchtracker.domain.hatchy.routing.*
import com.example.hatchtracker.domain.hatchy.routing.BreedingGoal
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TopicInterpretationTest {

    private lateinit var lexiconRegistry: LexiconRegistry
    private lateinit var topicEngine: TopicInferenceEngine

    @Before
    fun setup() {
        lexiconRegistry = LexiconRegistry()
        topicEngine = TopicInferenceEngine(lexiconRegistry)
    }

    @Test
    fun `test lexicon normalization`() {
        val raw = "How do I calibrate my TEMP sensor?"
        val normalized = lexiconRegistry.normalize(raw)
        assertEquals("how do i calibrate my temperature sensor", normalized)
    }

    @Test
    fun `test breeding topic inference`() {
        val query = "What is the best crossbreed for meat?"
        val result = topicEngine.inferTopics(query, emptyList(), realWorldMode())
        
        assertEquals(KnowledgeTopic.CROSSBREED_RECOMMENDATION, result.primaryTopic)
        assertTrue(lexiconRegistry.matchGoals(query).contains(BreedingGoal.MEAT_PRODUCTION))
        assertTrue(result.confidence > 0.8)
    }

    @Test
    fun `test incubation topic ambiguity`() {
        val query = "check temperature"
        val result = topicEngine.inferTopics(query, emptyList(), realWorldMode())
        
        // "temperature" maps to both TEMPERATURE and BROODER_TEMPERATURE
        assertTrue(result.topicScores.containsKey(KnowledgeTopic.TEMPERATURE))
        assertTrue(result.topicScores.containsKey(KnowledgeTopic.BROODER_TEMPERATURE))
    }

    @Test
    fun `test finance topic inference`() {
        val query = "how much have I spent on feed?"
        val result = topicEngine.inferTopics(query, emptyList(), appWorkflowMode())
        
        assertEquals(WorkflowTopic.LOG_EXPENSE, result.primaryTopic)
        // Note: "spent" matches LOG_EXPENSE in current lexicon
    }

    @Test
    fun `test nursery topic inference`() {
        val query = "when should I move chicks to the coop"
        val result = topicEngine.inferTopics(query, emptyList(), realWorldMode())
        
        assertEquals(KnowledgeTopic.READY_TO_MOVE, result.primaryTopic)
    }

    @Test
    fun `test multiple goals inference`() {
        val query = "dual purpose breed for show"
        val goals = lexiconRegistry.matchGoals(query)
        
        assertTrue(goals.contains(BreedingGoal.DUAL_PURPOSE))
        assertTrue(goals.contains(BreedingGoal.SHOW_ORNAMENTAL))
    }

    private fun realWorldMode() = QuestionModeResult(
        primaryMode = QuestionMode.REAL_WORLD_GUIDANCE,
        secondaryMode = null,
        modeConfidence = 1.0,
        appAnchorScore = 0.0,
        realWorldAnchorScore = 1.0,
        userDataAnchorScore = 0.0
    )

    private fun appWorkflowMode() = QuestionModeResult(
        primaryMode = QuestionMode.APP_WORKFLOW,
        secondaryMode = null,
        modeConfidence = 1.0,
        appAnchorScore = 1.0,
        realWorldAnchorScore = 0.0,
        userDataAnchorScore = 0.0
    )
}
