"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getVatRate = exports.EU_VAT_RATES = void 0;
/**
 * Standard EU VAT rates for digital services as of 2024/2025.
 *
 * NOTE: These rates should be periodically updated via a dynamic source
 * for production environments. This serves as a secure fallback table.
 */
exports.EU_VAT_RATES = {
    "AT": 20.0,
    "BE": 21.0,
    "BG": 20.0,
    "CY": 19.0,
    "CZ": 21.0,
    "DE": 19.0,
    "DK": 25.0,
    "EE": 22.0,
    "EL": 24.0,
    "ES": 21.0,
    "FI": 24.0,
    "FR": 20.0,
    "HR": 25.0,
    "HU": 27.0,
    "IE": 23.0,
    "IT": 22.0,
    "LT": 21.0,
    "LU": 17.0,
    "LV": 21.0,
    "MT": 18.0,
    "NL": 21.0,
    "PL": 23.0,
    "PT": 23.0,
    "RO": 19.0,
    "SE": 25.0,
    "SI": 22.0,
    "SK": 20.0 // Slovakia
};
/**
 * Helper to get rate by country code or default to 0 (No VAT).
 */
function getVatRate(countryCode) {
    return exports.EU_VAT_RATES[countryCode.toUpperCase()] || 0;
}
exports.getVatRate = getVatRate;
//# sourceMappingURL=vat_rates.js.map