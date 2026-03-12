package com.example.hatchtracker.domain.breeding

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyUtils {
    fun formatCurrency(amount: Double, currencyCode: String? = null): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        if (!currencyCode.isNullOrBlank()) {
            runCatching { Currency.getInstance(currencyCode) }
                .onSuccess { formatter.currency = it }
        }
        return formatter.format(amount)
    }
}
