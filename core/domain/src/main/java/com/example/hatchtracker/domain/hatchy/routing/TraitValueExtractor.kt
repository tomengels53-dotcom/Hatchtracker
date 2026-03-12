package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts multiple structured trait constraints and associated topics from queries.
 */
@Singleton
class TraitValueExtractor @Inject constructor() {

    /**
     * Extracts trait constraints found in the query.
     * Prioritizes longer matches (e.g., "dark brown eggs" over "brown eggs").
     */
    fun extract(query: String, speciesContext: PoultrySpecies? = null): List<TraitExtraction> {
        val q = query.lowercase().trim()
        val extractions = mutableListOf<TraitExtraction>()
        
        // Sort keys by length descending to match longest phrases first
        val sortedPhrases = TraitTopicLexicon.traitMappings.keys.sortedByDescending { it.length }
        
        var remainingQuery = q
        for (phrase in sortedPhrases) {
            if (remainingQuery.contains(phrase)) {
                val (topic, constraint) = TraitTopicLexicon.traitMappings[phrase]!!
                
                // Species sanity check (e.g., guarding geese vs guarding ducks)
                // For now, most traits in Lexicon are species-neutral or shared.
                
                extractions.add(TraitExtraction(
                    topic = topic,
                    constraint = constraint,
                    matchedPhrase = phrase,
                    isExactMatch = true
                ))
                
                // Replace matched phrase to avoid overlapping matches
                remainingQuery = remainingQuery.replace(phrase, " ".repeat(phrase.length))
            }
        }
        
        // Check for partial matches or synonyms if no exact matches found for a dimension?
        // Actually, Lexicon handles most synonyms by mapping to the same constraint.
        
        return extractions
    }
}

/**
 * Result of a trait extraction.
 */
data class TraitExtraction(
    val topic: KnowledgeTopic,
    val constraint: TraitConstraint,
    val matchedPhrase: String,
    val isExactMatch: Boolean
)
