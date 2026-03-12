package com.example.hatchtracker.domain.finance

data class FinanceSettings(
    val vatMode: VatMode = VatMode.DISABLED,
    val defaultVatRate: Double? = null, // e.g., 0.21
    val accountCurrency: String = "EUR"
)

enum class VatMode {
    DISABLED,
    SIMPLE,     // Flat VAT rate
    ADVANCED    // Multiple rates
}
