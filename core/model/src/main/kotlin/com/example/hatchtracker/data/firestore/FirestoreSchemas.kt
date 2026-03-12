package com.example.hatchtracker.data.firestore

/**
 * Firestore collection and field constants.
 * Documentation source: docs/FirestoreSchemas.md
 */
object FirestoreCollections {
    const val USERS = "users"
    const val DEVICES = "devices"
    const val FINANCIAL_ENTRIES = "financialEntries"
    const val FINANCIAL_SUMMARIES = "financialSummaries"

    const val TICKETS = "tickets"
    const val TICKET_MESSAGES = "messages"
    const val TICKET_INTERNAL_NOTES = "internal_notes"

    const val DEVICE_CATALOG = "deviceCatalog"
    const val BREED_STANDARDS = "breedStandards"
    const val INCUBATION_PROFILES = "incubationProfiles"
    const val AUDIT_LOGS = "auditLogs"
}

object FirestoreUserFields {
    const val COUNTRY_CODE = "countryCode"
    const val CURRENCY_CODE = "currencyCode"
    const val SUBSCRIPTION_TIER = "subscriptionTier"
    const val ADS_ENABLED = "adsEnabled"
    const val IS_SYSTEM_ADMIN = "isSystemAdmin"
    const val LAST_UPDATED = "lastUpdated"
    const val SUPPORT_TICKET_ID = "supportTicketId"
}

object FirestoreTicketFields {
    const val USER_ID = "userId"
    const val STATUS = "status"
    const val CATEGORY = "category"
    const val TYPE = "type"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
    const val PRIORITY = "priority"
    const val SUBSCRIPTION_TIER_AT_CREATION = "subscriptionTierAtCreation"

    const val APPROVED_AT = "approvedAt"
    const val APPROVED_BY = "approvedBy"
    const val CONSUMED_AT = "consumedAt"
    const val CONSUMED_BY = "consumedBy"
}

object FirestoreTicketCategory {
    const val PROFILE = "PROFILE"
    const val LOCALIZATION = "LOCALIZATION"
}

object FirestoreTicketType {
    const val COUNTRY_CHANGE = "COUNTRY_CHANGE"
    const val TRANSLATION_ERROR = "TRANSLATION_ERROR"
    const val MISSING_TRANSLATION = "MISSING_TRANSLATION"
}

object FirestoreTicketStatus {
    const val SUBMITTED = "SUBMITTED"
    const val IN_REVIEW = "IN_REVIEW"
    const val APPROVED = "APPROVED"
    const val RESOLVED = "RESOLVED"
    const val REJECTED = "REJECTED"
}
