package com.example.hatchtracker.domain.breeding.ui

import com.example.hatchtracker.model.breeding.*
import com.example.hatchtracker.model.UiText
import com.example.hatchtracker.model.genetics.InsightConfidence
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BreederActionUiMapper @Inject constructor() {

    /**
     * Maps semantic action categories and codes into display-ready UI advice.
     * Enforces confidence-aware phrasing (Direct vs Softer vs Cautionary).
     */
    fun mapSuggestion(suggestion: BreederActionSuggestion): BreederActionSuggestion {
        val phrasingPrefix = when (suggestion.confidence) {
            InsightConfidence.HIGH -> ""
            InsightConfidence.MODERATE -> "Consider: "
            InsightConfidence.LOW -> "Based on available data, consider: "
        }

        val originalText = when (val text = suggestion.suggestion) {
            is UiText.DynamicString -> text.value
            is UiText.StringResource -> "" // Phrasing not supported for resources in domain layer
        }

        return suggestion.copy(
            suggestion = UiText.DynamicString(phrasingPrefix + originalText)
        )
    }
}
