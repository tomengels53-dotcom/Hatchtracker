package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.HatchyTopic

/**
 * Interface for Lexical Matching.
 * Returns raw lexical matches based on keywords and patterns without semantic inference.
 */
interface ILexiconRegistry {
    /**
     * Returns raw lexical topic matches based on keywords.
     */
    fun matchTopics(query: String): Map<HatchyTopic, Double>

    /**
     * Returns raw lexical goal matches.
     */
    fun matchGoals(query: String): List<BreedingGoal>

    /**
     * Returns raw lexical anchor matches for system-level intents.
     */
    fun matchAnchors(query: String): Map<String, Double>
    
    /**
     * Normalizes the query for consistent matching.
     */
    fun normalize(query: String): String
}
