package com.example.hatchtracker.domain.policy

/**
 * Configuration for Privacy Policy and Terms of Service.
 * Allows for versioning and easy updates without code changes in UI logic.
 */
object LegalConfig {
    
    // Update these versions when the legal text changes.
    // The app will prompt users to re-accept if their stored version < current version.
    const val TERMS_VERSION = "1.1.0"
    const val PRIVACY_VERSION = "1.1.0"
    
    // Production URLs (Firebase Hosting)
    const val TERMS_OF_SERVICE_URL = "https://hatchbase.io/terms/"
    const val PRIVACY_POLICY_URL = "https://hatchbase.io/privacy/"
    
    // Support Email
    const val SUPPORT_EMAIL = "support@hatchbase.io"
    
    // Helper to check if re-acceptance is needed
    fun requiresReAcceptance(userTermsVersion: String?, userPrivacyVersion: String?): Boolean {
        // If current version is different from user version, assume update needed.
        // In robust systems, use semantic version comparison.
        return userTermsVersion != TERMS_VERSION || userPrivacyVersion != PRIVACY_VERSION
    }
}

