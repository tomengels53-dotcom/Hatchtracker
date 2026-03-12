# Email System Schema

This document defines the Firestore structure for tracking transactional email lifecycle and the outbound mail queue, optimized for use with the Firebase "Trigger Email" extension.

## 1. Email Event Audit Log
**Collection**: `emailEvents/{eventId}`
**Purpose**: Permanent audit trail of every email triggered by the system.
**Access**: User Read / Admin Write.

```typescript
{
  uid: string;            // Recipient user ID
  email: string;          // Recipient email address at time of trigger
  type: "purchase_confirmation" | "invoice" | "renewal_reminder" | "expiration" | "downgrade";
  status: "pending" | "sent" | "failed";
  error?: string;         // Error message if status is 'failed'
  createdAt: timestamp;   // Time the event was logged
  metadata: {             // Context-specific data (e.g., invoice ID, product name)
    invoiceId?: string;
    productName?: string;
    expiryDate?: string;
  }
}
```

## 2. Outbound Mail Queue
**Collection**: `mail/{mailId}`
**Purpose**: Direct integration with Firebase "Trigger Email" extension.
**Access**: Admin SDK only (Strictly server-side).

```typescript
{
  to: string | string[];  // Recipient email(s)
  message: {
    subject: string;
    text?: string;
    html?: string;        // HTML content or template references
  };
  delivery?: {            // Updated by the extension after transmission
    state: "SUCCESS" | "ERROR" | "PENDING";
    error?: string;
    endTime?: timestamp;
  };
  metadata: {             // Links back to the audit log
    emailEventId: string;
  }
}
```

## Security Strategy
- **Client Side**: All write access denied. Users can query `emailEvents` where `uid == request.auth.uid`.
- **Trigger**: Server-side Cloud Functions are the sole entry point for creating these records.
