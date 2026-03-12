# EU VAT & Multi-Currency Schema

This document defines the Firestore structure for handling user tax profiles, multi-currency pricing, and immutable invoice records.

## 1. User Tax Profiles
**Collection**: `taxProfiles/{uid}`
**Purpose**: Stores a user's current tax residency and exemption status. 
**Access**: User Read / Admin Write.

```typescript
{
  countryCode: string;     // ISO 3166-1 alpha-2 (e.g., "FR", "DE")
  vatRate: number;        // Current applicable VAT rate (e.g., 20.0, 19.0)
  vatAmount: number;      // VAT portion of the last/main subscription
  isVatExempt: boolean;    // Whether the user is VAT exempt (e.g., B2B valid ID - future ext)
  source: "google_play" | "stripe"; // Merchant of Record / Billing Provider
  createdAt: timestamp;    // Profile creation or latest update
  evidence: {              // Audit evidence for residency determination
    ipAddress?: string;
    billingAddressCountry?: string;
    creditCardBinCountry?: string;
  }
}
```

## 2. Multi-Currency Pricing
**Collection**: `pricing/{productId}`
**Purpose**: Stores price configurations for different currencies and billing periods.
**Access**: Public Read / Admin Write.

```typescript
{
  productName: string;     // Internal name (e.g., "Expert Monthly")
  pricing: {
    [currencyCode: string]: { // EUR, USD, GBP
      amount: number;         // Gross Amount (e.g., 9.99)
    }
  };
  billingPeriod: "monthly" | "yearly";
  updatedAt: timestamp;
}
```

## 3. Immutable Invoices
**Collection**: `invoices/{invoiceId}`
**Purpose**: Permanent, immutable legal records of financial transactions and tax collected.
**Access**: User Read / Admin Write.

```typescript
{
  ownerId: string;         // Link to user profile
  netAmount: number;       // Price excluding VAT
  vatAmount: number;       // Tax collected
  grossAmount: number;     // Total price paid (Net + VAT)
  currency: string;        // ISO 4217 Currency Code (e.g., "EUR", "USD")
  vatCountry: string;      // ISO 3166-1 alpha-2 where VAT was remitted
  vatRate: number;         // Rate applied at time of purchase
  providerInvoiceId: string; // External Reference (Google Order ID / Stripe Inv ID)
  billingSource: "google_play" | "stripe";
  createdAt: timestamp;    // Locked timestamp of record creation
  retentionUntil: timestamp; // audit requirement (usually createdAt + 10 years)
}
```

## Security Strategy
- **Client Side**: Denied write access to both collections.
- **Admin SDK**: Solely responsible for invoice and profile reconciliation.
