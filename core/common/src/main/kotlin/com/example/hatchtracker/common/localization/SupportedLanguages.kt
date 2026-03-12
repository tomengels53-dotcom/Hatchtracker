package com.example.hatchtracker.common.localization

import androidx.annotation.StringRes
import com.example.hatchtracker.core.common.R

/**
 * Model representing a supported language in the application.
 */
data class SupportedLanguage(
    val tag: String?, // BCP-47 tag, null/empty for System Default
    @param:StringRes val labelRes: Int
)

/**
 * Single Source of Truth for all 12+1 supported languages.
 */
val SupportedLanguages = listOf(
    SupportedLanguage(null, R.string.language_system),
    SupportedLanguage("en", R.string.language_english),
    SupportedLanguage("nl", R.string.language_dutch),
    SupportedLanguage("de", R.string.language_german),
    SupportedLanguage("fr", R.string.language_french),
    SupportedLanguage("es", R.string.language_spanish),
    SupportedLanguage("it", R.string.language_italian),
    SupportedLanguage("pt", R.string.language_portuguese),
    SupportedLanguage("pl", R.string.language_polish),
    SupportedLanguage("sv", R.string.language_swedish),
    SupportedLanguage("cs", R.string.language_czech),
    SupportedLanguage("da", R.string.language_danish),
    SupportedLanguage("fi", R.string.language_finnish),
    SupportedLanguage("nb", R.string.language_norwegian)
)
