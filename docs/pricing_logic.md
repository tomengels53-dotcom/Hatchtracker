# Currency Pricing Logic

This document outlines the strategy for multi-currency support in the HatchTracker platform, focusing on accuracy, compliance, and user experience.

## 1. Supported Currencies
- **USD**: Default for rest of world.
- **EUR**: Primary for EU member states.
- **GBP**: Specific for the United Kingdom.

## 2. Selection Strategy
Currency selection is determined by a hierarchy of inputs:
1. **Billing Provider Data**: If the provider (e.g., Google Play) returns a specific currency charged, that is the source of truth.
2. **User Tax Residency**: The `countryCode` determined during VAT verification maps to the primary currency (e.g., DE -> EUR).
3. **Locale Fallback**: If no billing or residency data exists, use the device locale's default currency.

## 3. Implementation Rules
- **No Client-Side Conversion**: The app displays localized prices fetched from the `pricing` collection or billing provider. It never performs real-time currency conversion locally.
- **Immutable Historical Records**: Invoices store the currency at the time of purchase. Future changes to local base prices do not affect historical data.
- **Extensibility**: New currencies (e.g., AUD, CAD) can be added by creating entries in the `pricing` collection and updating the server-side mapping in Cloud Functions.

## 4. Billing Provider Alignment
- **Google Play**: Automatically handles localization based on the user's Store country. The Cloud Function extracts `priceCurrencyCode` if available.
- **External Providers**: Use the `pricing/{productId}` collection to retrieve the correct amount based on the user's residency.
