package com.example.hatchtracker.common.localization

import com.example.hatchtracker.core.common.R

enum class AppLanguage(
    val tag: String,
    val displayNameRes: Int
) {
    SYSTEM("", R.string.language_system),
    ENGLISH("en", R.string.language_english),
    DUTCH("nl", R.string.language_dutch),
    FRENCH("fr", R.string.language_french),
    SPANISH("es", R.string.language_spanish),
    PORTUGUESE("pt", R.string.language_portuguese),
    GERMAN("de", R.string.language_german),
    ITALIAN("it", R.string.language_italian),
    POLISH("pl", R.string.language_polish),
    SWEDISH("sv", R.string.language_swedish),
    CZECH("cs", R.string.language_czech),
    DANISH("da", R.string.language_danish),
    FINNISH("fi", R.string.language_finnish),
    NORWEGIAN("nb", R.string.language_norwegian);

    companion object {
        fun fromTag(tag: String): AppLanguage {
            if (tag.isBlank()) return SYSTEM
            return entries.firstOrNull { language ->
                tag.equals(language.tag, ignoreCase = true) ||
                    tag.startsWith("${language.tag}-", ignoreCase = true)
            } ?: SYSTEM
        }
    }
}
