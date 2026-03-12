package com.example.hatchtracker.common.format

import androidx.appcompat.app.AppCompatDelegate
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleFormatService @Inject constructor() {

    private fun getAppLocale(): Locale {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) {
            locales.get(0) ?: Locale.getDefault()
        } else {
            Locale.getDefault()
        }
    }

    fun formatDate(epochMillis: Long, pattern: String? = null): String {
        val date = Date(epochMillis)
        return if (pattern != null) {
            try {
                SimpleDateFormat(mapUserPattern(pattern), getAppLocale()).format(date)
            } catch (e: Exception) {
                SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, getAppLocale()).format(date)
            }
        } else {
            SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, getAppLocale()).format(date)
        }
    }

    fun formatDateTime(epochMillis: Long, datePattern: String? = null, timePattern: String? = null): String {
        val date = Date(epochMillis)
        return try {
            val dPattern = if (datePattern != null) mapUserPattern(datePattern) else null
            val tPattern = if (timePattern != null) mapUserTimePattern(timePattern) else null
            
            if (dPattern != null || tPattern != null) {
                // Combine patterns. Example: "dd-MM-yyyy HH:mm"
                val combined = "${dPattern ?: "MMM dd, yyyy"} ${tPattern ?: "HH:mm"}"
                SimpleDateFormat(combined, getAppLocale()).format(date)
            } else {
                SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.SHORT, getAppLocale()).format(date)
            }
        } catch (e: Exception) {
            SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.SHORT, getAppLocale()).format(date)
        }
    }

    fun formatDate(isoDateString: String, pattern: String? = null): String {
        return try {
            // ISO-8601 YYYY-MM-DD
            val parts = isoDateString.split("-")
            if (parts.size == 3) {
                val calendar = Calendar.getInstance()
                calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                formatDate(calendar.timeInMillis, pattern)
            } else {
                isoDateString
            }
        } catch (e: Exception) {
            isoDateString
        }
    }

    /**
     * Maps user-friendly patterns like "DD-MM-YYYY" to Java SimpleDateFormat patterns like "dd-MM-yyyy".
     */
    private fun mapUserPattern(pattern: String): String {
        return pattern
            .replace("DD", "dd")
            .replace("YYYY", "yyyy")
            .replace("MM", "MM") // Already correct
    }

    /**
     * Maps user-friendly time formats like "24h" or "12h" to patterns.
     */
    private fun mapUserTimePattern(format: String): String {
        return when (format.lowercase()) {
            "24h" -> "HH:mm"
            "12h" -> "hh:mm a"
            else -> format
        }
    }

    fun formatNumber(value: Double, maxFractionDigits: Int = 2): String {
        val format = NumberFormat.getNumberInstance(getAppLocale())
        format.maximumFractionDigits = maxFractionDigits
        return format.format(value)
    }

    fun formatPercent(value: Double): String {
        val format = NumberFormat.getPercentInstance(getAppLocale())
        return format.format(value)
    }

    /**
     * Formats currency using minor units (e.g., cents).
     */
    fun formatCurrency(minorUnits: Long, currencyCode: String): String {
        val amount = minorUnits / 100.0
        val currency = Currency.getInstance(currencyCode)
        val format = NumberFormat.getCurrencyInstance(getAppLocale())
        format.currency = currency
        return format.format(amount)
    }

    fun formatCurrency(amount: Double, currencyCode: String): String {
        return formatCurrency((amount * 100).toLong(), currencyCode)
    }

    /**
     * Formats weight based on grams and user preference.
     */
    fun formatWeight(grams: Long, useLbs: Boolean): String {
        return if (useLbs) {
            val lbs = grams / 453.59237
            "${formatNumber(lbs)} lbs" // Will be externalized in UI layer
        } else {
            val kg = grams / 1000.0
            "${formatNumber(kg)} kg"
        }
    }
}
