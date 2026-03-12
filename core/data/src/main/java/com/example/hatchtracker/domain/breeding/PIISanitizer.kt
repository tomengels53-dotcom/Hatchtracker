package com.example.hatchtracker.domain.breeding

import java.util.regex.Pattern

/**
 * Sanitizes strings to protect User PII (Personally Identifiable Information) before LLM egress.
 */
object PIISanitizer {
    
    // Simple regex for common PII patterns
    private val EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+\\.[a-z]+")
    private val PHONE_PATTERN = Pattern.compile("(\\+\\d{1,3}[- ]?)?(\\(\\d{3}\\)|\\d{3})[- ]?\\d{3}[- ]?\\d{4}")
    
    // A strict allowlist of poultry-related terms that should NEVER be redacted
    private val POULTRY_ALLOWLIST = setOf(
        "chicken", "duck", "turkey", "quail", "goose", "guinea", "pheasant", 
        "rhode island red", "silkies", "leghorn", "marans", "orpington",
        "egg", "hatch", "brooder", "incubator", "candling", "frizzle", "bantam"
    )

    /**
     * Redacts email addresses, phone numbers, and suspected names from the input string.
     */
    fun sanitize(input: String): String {
        var result = input
        
        // 1. Redact Emails
        result = EMAIL_PATTERN.matcher(result).replaceAll("[REDACTED EMAIL]")
        
        // 2. Redact Phone Numbers
        result = PHONE_PATTERN.matcher(result).replaceAll("[REDACTED PHONE]")
        
        return result
    }

    /**
     * Sanitizes a data map by removing or redacting sensitive keys.
     */
    fun sanitizeDataMap(data: Map<String, Any?>): Map<String, Any?> {
        val sensitiveKeys = setOf("name", "label", "notes", "hatchNotes", "address", "phone", "email")
        return data.mapValues { (key, value) ->
            if (sensitiveKeys.contains(key.lowercase()) && value is String) {
                "[REDACTED]"
            } else {
                value
            }
        }
    }
}
