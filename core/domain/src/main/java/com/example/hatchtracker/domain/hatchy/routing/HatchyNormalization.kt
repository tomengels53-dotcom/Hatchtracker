package com.example.hatchtracker.domain.hatchy.routing

/**
 * Centralized normalization logic for Hatchy.
 * Standardizes raw user text for deterministic matching.
 */
object HatchyNormalization {

    /**
     * Aggressively cleans and normalizes text for lexicon matching.
     */
    fun normalize(query: String): String {
        var text = query.lowercase().replace(Regex("[^a-z0-9\\s]"), " ")
        
        // 1. Common abbreviations & shorthands
        text = text.replace(Regex("\\btemp\\b"), "temperature")
        text = text.replace(Regex("\\binfo\\b"), "information")
        text = text.replace(Regex("\\bgen\\b"), "generation")
        text = text.replace(Regex("\\brir\\b"), "rhode island red")
        text = text.replace(Regex("\\bjg\\b"), "jersey giant")
        text = text.replace(Regex("\\bls\\b"), "light sussex")
        text = text.replace(Regex("\\bwlh\\b"), "white leghorn")
        text = text.replace(Regex("\\bbr\\b"), "barred rock")
        text = text.replace(Regex("\\bf2\\b"), "2nd generation")
        text = text.replace(Regex("\\bf1\\b"), "1st generation")

        // 2. Domain-specific term merging
        text = text.replace(Regex("\\block down\\b"), "lockdown")
        text = text.replace(Regex("\\bhatchday\\b"), "hatch day")
        
        // 3. Simple singularization
        text = text.replace(Regex("eggs\\b"), "egg")
        text = text.replace(Regex("chicks\\b"), "chick")
        text = text.replace(Regex("birds\\b"), "bird")
        text = text.replace(Regex("crosses\\b"), "cross")
        text = text.replace(Regex("breeds\\b"), "breed")
        text = text.replace(Regex("spending\\b"), "spend")
        text = text.replace(Regex("costs\\b"), "cost")
        text = text.replace(Regex("hens\\b"), "hen")
        text = text.replace(Regex("roosters\\b"), "rooster")
        text = text.replace(Regex("ducks\\b"), "duck")

        return text.replace(Regex("\\s+"), " ").trim()
    }
}

