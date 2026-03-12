package com.example.hatchtracker.domain.breeding

/**
 * Data class representing default localization settings for a country.
 */
data class CountryDefaults(
    val name: String,
    val code: String, // ISO 3166-1 alpha-2
    val weightUnit: String,
    val dateFormat: String,
    val timeFormat: String,
    val currencyCode: String
)

/**
 * Utility for providing default localization settings based on country.
 */
object LocalizationDefaults {
    val countries = listOf(
        CountryDefaults("United States", "US", "lbs", "YYYY-MM-DD", "12h", "USD"),
        CountryDefaults("Belgium", "BE", "kg", "DD-MM-YYYY", "24h", "EUR"),
        CountryDefaults("United Kingdom", "GB", "kg", "DD-MM-YYYY", "24h", "GBP"),
        CountryDefaults("Canada", "CA", "kg", "YYYY-MM-DD", "24h", "CAD"),
        CountryDefaults("Australia", "AU", "kg", "DD-MM-YYYY", "24h", "AUD"),
        CountryDefaults("France", "FR", "kg", "DD-MM-YYYY", "24h", "EUR"),
        CountryDefaults("Germany", "DE", "kg", "DD-MM-YYYY", "24h", "EUR"),
        CountryDefaults("Netherlands", "NL", "kg", "DD-MM-YYYY", "24h", "EUR"),
        CountryDefaults("Spain", "ES", "kg", "DD-MM-YYYY", "24h", "EUR"),
        CountryDefaults("Italy", "IT", "kg", "DD-MM-YYYY", "24h", "EUR"),
        CountryDefaults("Japan", "JP", "kg", "YYYY-MM-DD", "24h", "JPY"),
        CountryDefaults("China", "CN", "kg", "YYYY-MM-DD", "24h", "CNY"),
        CountryDefaults("India", "IN", "kg", "DD-MM-YYYY", "24h", "INR"),
        CountryDefaults("Brazil", "BR", "kg", "DD-MM-YYYY", "24h", "BRL"),
        CountryDefaults("South Africa", "ZA", "kg", "YYYY-MM-DD", "24h", "ZAR")
    ).sortedBy { it.name }

    /**
     * Get defaults for a specific country name or ISO code.
     */
    fun getDefaultsForCountry(query: String): CountryDefaults {
        return countries.find { it.name.equals(query, ignoreCase = true) || it.code.equals(query, ignoreCase = true) } 
            ?: CountryDefaults("Other", "XX", "kg", "DD-MM-YYYY", "24h", "USD")
    }
}
