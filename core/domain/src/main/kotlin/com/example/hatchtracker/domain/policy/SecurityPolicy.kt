package com.example.hatchtracker.domain.policy

import java.util.regex.Pattern

/**
 * Defines the minimum security requirements for the application.
 */
object SecurityPolicy {

    // -----------------------------------------------------------
    // PASSWORD REQUIREMENTS
    // -----------------------------------------------------------
    const val MIN_PASSWORD_LENGTH = 8
    
    // At least one digit, one lowercase, one uppercase, one special char, no whitespace
    // Simplification: At least 8 chars, one letter, one number.
    private val PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$")

    fun isValidPassword(password: String): Boolean {
        return password.length >= MIN_PASSWORD_LENGTH && PASSWORD_PATTERN.matcher(password).matches()
    }

    fun getPasswordRequirementsDescription(): String {
        return "Password must be at least $MIN_PASSWORD_LENGTH characters long and include at least one letter and one number."
    }

    // -----------------------------------------------------------
    // SESSION SECURITY
    // -----------------------------------------------------------
    // Firebase Auth handles token refresh (1 hour).
    // Application-level session timeout (optional, for banking-like apps):
    const val INACTIVITY_TIMEOUT_MS = 15 * 60 * 1000L // 15 Minutes
    
    // Critical actions requiring re-authentication
    const val REAUTH_REQUIRED_FOR_DELETE_ACCOUNT = true
    const val REAUTH_REQUIRED_FOR_EXPORT_DATA = true
}

