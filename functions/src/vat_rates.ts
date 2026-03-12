/**
 * Standard EU VAT rates for digital services as of 2024/2025.
 * 
 * NOTE: These rates should be periodically updated via a dynamic source
 * for production environments. This serves as a secure fallback table.
 */
export const EU_VAT_RATES: Record<string, number> = {
    "AT": 20.0, // Austria
    "BE": 21.0, // Belgium
    "BG": 20.0, // Bulgaria
    "CY": 19.0, // Cyprus
    "CZ": 21.0, // Czech Republic
    "DE": 19.0, // Germany
    "DK": 25.0, // Denmark
    "EE": 22.0, // Estonia (Increased in 2024/25)
    "EL": 24.0, // Greece
    "ES": 21.0, // Spain
    "FI": 24.0, // Finland
    "FR": 20.0, // France
    "HR": 25.0, // Croatia
    "HU": 27.0, // Hungary
    "IE": 23.0, // Ireland
    "IT": 22.0, // Italy
    "LT": 21.0, // Lithuania
    "LU": 17.0, // Luxembourg
    "LV": 21.0, // Latvia
    "MT": 18.0, // Malta
    "NL": 21.0, // Netherlands
    "PL": 23.0, // Poland
    "PT": 23.0, // Portugal
    "RO": 19.0, // Romania
    "SE": 25.0, // Sweden
    "SI": 22.0, // Slovenia
    "SK": 20.0  // Slovakia
};

/**
 * Helper to get rate by country code or default to 0 (No VAT).
 */
export function getVatRate(countryCode: string): number {
    return EU_VAT_RATES[countryCode.toUpperCase()] || 0;
}
