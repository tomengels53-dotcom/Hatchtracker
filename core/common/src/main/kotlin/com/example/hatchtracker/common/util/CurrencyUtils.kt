package com.example.hatchtracker.common.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    fun formatCurrency(amount: Double, currencyCode: String? = null): String {
        val formatter = if (currencyCode != null) {
            val currency = java.util.Currency.getInstance(currencyCode)
            val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
            format.currency = currency
            format
        } else {
            NumberFormat.getCurrencyInstance(Locale.getDefault())
        }
        return formatter.format(amount)
    }

    fun getLocalCurrencyCode(): String {
        return try {
            java.util.Currency.getInstance(Locale.getDefault()).currencyCode
        } catch (e: Exception) {
            "USD"
        }
    }

    fun getLocalCountryCode(): String {
        return Locale.getDefault().country ?: "US"
    }
}
