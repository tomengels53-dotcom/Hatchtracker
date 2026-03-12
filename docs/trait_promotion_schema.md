# Trait Promotion Schema

This document defines the Firestore structure for community-driven trait verification and official breed standard updates.

## 1. Trait Observations
**Collection**: `traitObservations/{obsId}`
**Purpose**: Stores individual user reports of observed traits on specific birds.
**Access**: User Create / Admin Read.

```typescript
{
  uid: string;            // User who made the observation
  birdId: string;         // Bird ID (Long/String ref)
  breedId: string;        // Breed ID of the bird
  traitId: string;        // ID of the trait observed
  observedAt: timestamp;  // Creation time
  evidenceUrl?: string;   // Optional link to photo evidence
}
```

## 2. Trait Promotion Requests
**Collection**: `traitPromotionRequests/{reqId}`
**Purpose**: Aggregated data for traits that have met community observation thresholds.
**Access**: Admin Only.

```typescript
{
  traitId: string;
  breedId: string;
  evidenceCount: number;  // Number of unique observations
  confidenceScore: number; // Calculated based on unique contributors/volume
  status: "pending" | "approved" | "rejected";
  createdAt: timestamp;
  lastUpdatedAt: timestamp;
  adminNotes?: string;
}
```

## Security Strategy
- **Observations**: Any authenticated user can submit an observation. Users can read their own observations.
- **Aggregation**: Cloud Functions periodically (or on-trigger) aggregate observations to avoid client-side manipulation of promotion status.
- **Approvals**: Strictly reserved for users with the `isAdmin` claim.
