package com.example.hatchtracker.domain.hatchy

import com.example.hatchtracker.domain.hatchy.routing.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResponsePacingPolicyTest {

    private lateinit var policy: ResponsePacingPolicy

    @Before
    fun setup() {
        policy = ResponsePacingPolicy()
    }

    @Test
    fun `test thinking label for navigation`() {
        val interpretation = mockInterpretation(intent = HatchyIntent.APP_NAVIGATION)
        val label = policy.determineThinkingLabel(interpretation)
        assertEquals("Finding the right screen...", label)
    }

    @Test
    fun `test thinking label for breeding with user data`() {
        val interpretation = mockInterpretation(
            intent = HatchyIntent.BREEDING_GUIDANCE,
            userDataScore = 0.9
        )
        val label = policy.determineThinkingLabel(interpretation)
        assertEquals("Checking your flock...", label)
    }

    @Test
    fun `test thinking label for breeding without user data`() {
        val interpretation = mockInterpretation(
            intent = HatchyIntent.BREEDING_GUIDANCE,
            userDataScore = 0.1
        )
        val label = policy.determineThinkingLabel(interpretation)
        assertEquals("Preparing breeding guidance...", label)
    }

    @Test
    fun `test minimum delay for recommendation`() {
        val answer = mockAnswer(type = AnswerType.RECOMMENDATION)
        val delay = policy.calculateMinimumDelay(answer)
        assertEquals(1200L, delay)
    }

    @Test
    fun `test minimum delay for navigation`() {
        val answer = mockAnswer(type = AnswerType.NAVIGATION)
        val delay = policy.calculateMinimumDelay(answer)
        assertEquals(300L, delay)
    }

    @Test
    fun `test minimum delay for user data source`() {
        val answer = mockAnswer(source = AnswerSource.USER_DATA)
        val delay = policy.calculateMinimumDelay(answer)
        assertEquals(800L, delay)
    }

    private fun mockInterpretation(
        intent: HatchyIntent = HatchyIntent.OTHER,
        userDataScore: Double = 0.0
    ): QueryInterpretation {
        return QueryInterpretation(
            rawQuery = "test",
            questionMode = QuestionModeResult(QuestionMode.UNKNOWN, null, 0.1, 0.1, 0.1, userDataScore),
            entities = emptyList(),
            intent = intent,
            topicResult = TopicInferenceResult(null, null, emptyMap(), 0.0),
            inferredGoals = emptyList(),
            confidence = 0.5
        )
    }

    private fun mockAnswer(
        type: AnswerType = AnswerType.FALLBACK,
        source: AnswerSource = AnswerSource.FALLBACK
    ): HatchyAnswer {
        return HatchyAnswer(
            text = "test",
            type = type,
            confidence = AnswerConfidence.MEDIUM,
            source = source
        )
    }
}
