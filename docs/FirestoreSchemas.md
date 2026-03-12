# Firestore Schemas (C9)

This document is the source of truth for Firestore collection schemas, required fields, and indexing notes. It reflects current modularized app usage and the active `firestore.rules` (see repository root).

Notes
- All timestamps are Unix epoch millis (`Long`).
- All documents should include `lastUpdated` when updated.
- Security rules currently only cover `/users`, `/users/*/devices`, `/users/*/financialEntries`, `/users/*/financialSummaries`, `/incubations`, `/deviceCatalog`, and `/tickets` (+ subcollections). All other collections are denied by default unless rules are expanded.

## Collections

### `/users/{uid}`
Required fields
- `countryCode` `String` (ISO 3166-1 alpha-2)
- `currencyCode` `String` (ISO 4217)
- `subscriptionTier` `String` (`FREE`, `EXPERT`, `PRO`)
- `adsEnabled` `Boolean`
- `lastUpdated` `Long`

Optional fields
- `isSystemAdmin` `Boolean` (mirror for admin tooling; auth claim is still source of truth)
- `supportTicketId` `String` (required only for country change flow)

Security rules
- Read: authenticated user can read own document.
- Create: authenticated user can create own document; `currencyCode` and `countryCode` must be present.
- Update: authenticated user can update own document, with country/currency restricted unless admin or support ticket present. Only `lastUpdated`, `countryCode`, `currencyCode` may change when using `supportTicketId`.

Indexing notes
- Single-field indexes are sufficient for primary access by document ID.

### `/users/{uid}/devices/{deviceId}`
Required fields
- `userId` `String` (must match `{uid}`)
- `capacityEggs` `Number` (must be >= 0)

Optional fields
- `displayName` `String`
- `type` `String` (e.g. `INCUBATOR`, `HATCHER`)
- `features` `Map<String, Boolean>`
- `createdAt` `Long`
- `lastUpdated` `Long`

Security rules
- Read/Write: authenticated user can read/write own devices.
- Create/Update: `capacityEggs` must be >= 0 and `userId` must match `{uid}`.

Indexing notes
- If querying by `type`, add a single-field index on `type`.

### `/users/{uid}/financialEntries/{entryId}`
Required fields
- `ownerId` `String`
- `ownerType` `String` (`flock`, `incubation`, `flocklet`)
- `amount` `Number`
- `isRevenue` `Boolean`
- `createdAt` `Long`

Optional fields
- `notes` `String`
- `category` `String`
- `lastUpdated` `Long`

Security rules
- Read/Write: authenticated user can read/write own financial entries.

Indexing notes
- Composite index recommended for queries by `ownerType` + `ownerId` ordered by `createdAt`.

### `/users/{uid}/financialSummaries/{summaryId}`
Required fields
- `ownerId` `String`
- `ownerType` `String`
- `netProfit` `Number`
- `totalRevenue` `Number`
- `totalCost` `Number`
- `updatedAt` `Long`

Security rules
- Read/Write: authenticated user can read/write own financial summaries.

Indexing notes
- Typically accessed by document ID; no additional indexes required.

### `/tickets/{ticketId}`
Required fields
- `userId` `String`
- `status` `String` (enum values below)
- `category` `String` (enum values below)
- `type` `String` (enum values below)
- `createdAt` `Long`
- `updatedAt` `Long`
- `priority` `String` (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`)
- `subscriptionTierAtCreation` `String` (`FREE`, `EXPERT`, `PRO`)

Optional fields
- `approvedAt` `Long`
- `approvedBy` `String`
- `consumedAt` `Long`
- `consumedBy` `String`
- `subject` `String`
- `description` `String`
- `changeRequest` `Map<String, Any>`

Ticket enums
- `status`: `SUBMITTED`, `IN_REVIEW`, `APPROVED`, `RESOLVED`, `REJECTED`
- `category`: `PROFILE` (for account changes and country edits)
- `type`: `COUNTRY_CHANGE` (for country edits)

Security rules
- Create: authenticated user can create own ticket.
- Read: owner can read own ticket; system admin can read all.
- Update/Delete: system admin only.

Indexing notes
- Composite index recommended for queries by `userId` ordered by `createdAt`.
- Composite index recommended for admin lists ordered by `createdAt`.

Subcollections
- `/tickets/{ticketId}/messages/{messageId}`
  - Fields: `authorId` `String`, `content` `String`, `createdAt` `Long`, `isInternal` `Boolean`
  - Rules: admin can read/write all; owner can read/write only non-internal messages; update/delete denied.
- `/tickets/{ticketId}/internal_notes/{noteId}`
  - Fields: `authorId` `String`, `content` `String`, `createdAt` `Long`
  - Rules: admin only.

### `/deviceCatalog/**`
Required fields
- `type` `String` (e.g. `INCUBATOR`, `HATCHER`)
- `displayName` `String`
- `capacityEggs` `Number`

Security rules
- Read: authenticated users.
- Write: denied (admin via console/backend).

Indexing notes
- Single-field index on `type` recommended if filtering by device type.

### `/breedStandards/{id}`
Required fields
- `breedId` `String`
- `species` `String`
- `name` `String`

Optional fields
- `traits` `Map<String, Any>`
- `updatedAt` `Long`

Security rules
- Read/write allowed for system admins (see `firestore.rules`).

Indexing notes
- Single-field index on `species` recommended.

### `/incubationProfiles/{speciesId}`
Required fields
- `speciesId` `String`
- `profile` `Map<String, Any>`

Security rules
- Not covered in current `firestore.rules` (access denied by default).

### `/auditLogs/{id}`
Required fields
- `type` `String`
- `adminId` `String`
- `userId` `String`
- `timestamp` `Long`

Optional fields
- `ticketId` `String`
- `oldValue` `String`
- `newValue` `String`
- `metadata` `Map<String, Any>`

Security rules
- Not covered in current `firestore.rules` (access denied by default).

## Additional Collections Used by Current App Logic

These are used in code but not covered in `firestore.rules` yet. Access will be denied unless rules are expanded.
- `/legalDisclaimers/{id}`
- `/users/{uid}/disclaimerAcks/{id}`
- `/traitObservations/{id}`
- `/traitVotes/{id}`
- `/breedingScenarios/{id}`
- `/breedingScenarios/{id}/generations/{generationId}`
- `/breedingSimulations/{id}`
- `/breedingSimulations/{id}/generations/{generationId}`

## Security Rules Alignment (Current)

Summary of active rules in `firestore.rules`
- `/users/{uid}` read/write for owner, with restricted country/currency updates.
- `/users/{uid}/devices`, `/financialEntries`, `/financialSummaries` read/write for owner.
- `/incubations/{id}` read/write for owner (not currently used in app code).
- `/deviceCatalog/**` read for auth users.
- `/tickets/**` read for owner/admin, write/update for admin only (except create by owner); messages have internal visibility rules.
- `/auditLogs/{id}` read/write for system admins only.

If collections in the "Additional Collections Used" list are intended to be active, rules must be extended to avoid access denials.
