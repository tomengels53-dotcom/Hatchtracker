package com.example.hatchtracker.domain.hatchy

import com.example.hatchtracker.domain.hatchy.model.HatchyContext
import com.example.hatchtracker.domain.hatchy.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

@Singleton
class HatchyOrchestrator @Inject constructor(
    private val classifier: HatchyIntentClassifier,
    private val questionModeClassifier: QuestionModeClassifier,
    private val entityExtractor: HatchyEntityExtractor,
    private val contextBuilder: HatchyContextSnapshotBuilder,
    private val resolverRegistry: HatchyResolverRegistry,
    private val responseComposer: HatchyResponseComposer,
    private val confidenceEvaluator: HatchyConfidenceEvaluator,
    private val topicInferenceEngine: TopicInferenceEngine,
    private val pacingPolicy: ResponsePacingPolicy
) {

    /**
     * Legacy support for single-shot processing.
     */
    suspend fun processQuery(
        query: String,
        appContext: HatchyContext,
        locale: String = "en"
    ): HatchyAnswer {
        val interpretation = interpret(query, locale)
        val fullIntentResult = HatchyIntentResult(
            intent = interpretation.intent,
            module = interpretation.module,
            entities = interpretation.entities,
            confidence = interpretation.confidence,
            questionModeResult = interpretation.questionMode
        )
        val contextSnapshot = contextBuilder.build(appContext, fullIntentResult)
        return resolveAndCompose(interpretation, contextSnapshot, fullIntentResult)
    }

    /**
     * Modern Flow-based processing with deterministic pacing and thinking labels.
     */
    fun processQueryFlow(
        query: String,
        appContext: HatchyContext,
        locale: String = "en"
    ): Flow<HatchyProcessEvent> = flow {
        val startTime = TimeSource.Monotonic.markNow()

        // 1. Initial Interpretation (fast)
        val interpretation = interpret(query, locale)
        val fullIntentResult = HatchyIntentResult(
            intent = interpretation.intent,
            module = interpretation.module,
            entities = interpretation.entities,
            confidence = interpretation.confidence,
            questionModeResult = interpretation.questionMode
        )

        // 2. Emit Thinking State
        val label = pacingPolicy.determineThinkingLabel(interpretation)
        emit(HatchyProcessEvent.Thinking(label))

        // 3. Resolve Answer
        val contextSnapshot = contextBuilder.build(appContext, fullIntentResult)
        val answer = resolveAndCompose(interpretation, contextSnapshot, fullIntentResult)

        // 4. Calculate Pacing Offset
        val minDelay = pacingPolicy.calculateMinimumDelay(answer).milliseconds
        val elapsed = startTime.elapsedNow()
        val remaining = minDelay - elapsed

        if (remaining.isPositive()) {
            delay(remaining)
        }

        // 5. Done
        emit(HatchyProcessEvent.Done(answer))
    }

    private fun interpret(query: String, locale: String): QueryInterpretation {
        val qModeResult = questionModeClassifier.classify(query)
        val intentResult = classifier.classify(query, locale)
        val entities = entityExtractor.extract(query)
        val topicResult = topicInferenceEngine.inferTopics(query, entities, qModeResult)

        return QueryInterpretation(
            rawQuery = query,
            questionMode = qModeResult,
            entities = entities,
            intent = intentResult.intent,
            topicResult = topicResult,
            inferredGoals = emptyList(),
            confidence = (intentResult.confidence + topicResult.confidence) / 2.0,
            module = intentResult.module
        )
    }

    private suspend fun resolveAndCompose(
        interpretation: QueryInterpretation,
        contextSnapshot: HatchyContextSnapshot,
        fullIntentResult: HatchyIntentResult
    ): HatchyAnswer {
        val allResolvers = resolverRegistry.getAllResolvers()
        
        val candidates = allResolvers
            .map { it to it.score(interpretation, contextSnapshot) }
            .filter { it.second.finalScore > 0.1 } 
            .sortedWith(
                compareByDescending<Pair<HatchyResolver, ScoreResult>> { it.second.finalScore }
                    .thenByDescending { if (it.second.entityRequirementSatisfied) 1 else 0 }
                    .thenByDescending { if (it.first.capabilities.preferredQuestionModes.contains(interpretation.questionMode.primaryMode)) 1 else 0 }
                    .thenByDescending { if (it.first.capabilities.requiresUserData && it.second.components.userDataScore > 0.5) 1 else 0 }
                    .thenByDescending { it.first.priority }
            )
            .map { it.first }

        var resolvedAnswer: HatchyAnswer? = null
        val rejectedMap = mutableMapOf<String, String>()

        for (resolver in candidates) {
            when (val outcome = resolver.resolve(interpretation, contextSnapshot)) {
                is ResolverOutcome.Resolved -> {
                    resolvedAnswer = outcome.answer
                    break
                }
                is ResolverOutcome.InsufficientEvidence -> {
                    if (outcome.fallbackAnswer != null && resolvedAnswer == null) {
                        resolvedAnswer = outcome.fallbackAnswer
                    }
                }
                is ResolverOutcome.NotApplicable -> {
                    rejectedMap[resolver.javaClass.simpleName] = "NotApplicable"
                }
            }
        }

        val initialAnswer = resolvedAnswer ?: HatchyAnswer(
            text = "I'm not quite sure how to help with that specifically. Can you try rephrasing?",
            type = AnswerType.FALLBACK,
            confidence = AnswerConfidence.LOW,
            source = AnswerSource.FALLBACK
        )

        val finalAnswer = responseComposer.compose(initialAnswer, contextSnapshot, interpretation)

        // Trace for observability
        val winningScore = candidates.firstOrNull()?.let { r -> allResolvers.find { it == r }?.score(interpretation, contextSnapshot) }
        generateTrace(interpretation.rawQuery, interpretation, candidates, rejectedMap, finalAnswer, winningScore)

        return finalAnswer
    }

    private fun generateTrace(
        query: String,
        interpretation: QueryInterpretation,
        candidates: List<HatchyResolver>,
        rejected: Map<String, String>,
        answer: HatchyAnswer,
        scoreResult: ScoreResult? = null
    ) {
        val qMode = interpretation.questionMode
        val trace = RoutingTrace(
            query = query,
            primaryQuestionMode = qMode.primaryMode.name,
            secondaryQuestionMode = qMode.secondaryMode?.name,
            modeConfidence = qMode.modeConfidence,
            appAnchorScore = qMode.appAnchorScore,
            realWorldAnchorScore = qMode.realWorldAnchorScore,
            userDataAnchorScore = qMode.userDataAnchorScore,
            classifiedDomain = interpretation.intent.name,
            selectedSubtype = answer.debugMetadata?.get("matchedSubtype") as? String,
            candidates = candidates.map { it.javaClass.simpleName },
            rejected = rejected,
            selectedResolver = answer.debugMetadata?.get("resolver") as? String ?: answer.source.name,
            confidenceInputs = mapOf(
                "intent" to interpretation.confidence,
                "finalScore" to (scoreResult?.finalScore ?: 0.0)
            ),
            evidenceKeys = answer.debugMetadata?.keys?.toList() ?: emptyList(),
            scoreComponents = scoreResult?.components?.let {
                mapOf(
                    "topic" to it.topicMatchScore,
                    "entity" to it.entityScore,
                    "mode" to it.questionModeScore,
                    "userData" to it.userDataScore,
                    "context" to it.contextContinuityScore
                )
            } ?: emptyMap()
        )
        println("Hatchy Trace: $trace")
    }
}
