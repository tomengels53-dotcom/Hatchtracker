# Firestore Indexes

This document lists the required composite indexes for the HatchTracker application.
Single-field indexes are automatically managed by Firestore and are typically not listed here unless exemptions are required.

## Collection: `tickets`

### Query: User Ticket History
- **Usage**: `SupportRepository.getTicketsForUser(userId)`
- **Query**: `.whereEqualTo("userId", userId).orderBy("createdAt", DESCENDING)`
- **Index Required**:
  - Field: `userId` (Ascending)
  - Field: `createdAt` (Descending)
  - Scope: Collection

### Query: Admin Ticket Dashboard
- **Usage**: `SupportRepository.getAllTickets()`
- **Query**: `.orderBy("createdAt", DESCENDING)`
- **Index Required**:
  - *Note*: This is a single field sort, typically supported by default indexes. However, if filtering by status is added later (e.g., `whereEqualTo("status", "SUBMITTED").orderBy("createdAt")`), a composite index will be needed.
  - **Recommendation**: Keep default single-field index for now.

## Collection: `tickets/{ticketId}/internal_notes`

### Query: Internal Notes
- **Usage**: `SupportRepository.getInternalNotes(ticketId)`
- **Query**: `.orderBy("createdAt", ASCENDING)`
- **Index Required**: Default single-field index is sufficient.

## Collection: `tickets/{ticketId}/messages`

### Query: Chat Messages
- **Usage**: `SupportRepository.getMessages(ticketId)`
- **Query**: `.orderBy("createdAt", ASCENDING)`
- **Index Required**: Default single-field index is sufficient.

## Collection: `breedingSimulations`

### Query: User Simulations
- **Usage**: `SimulationRepository.getUserSimulations(userId)`
- **Query**: `.whereEqualTo("ownerId", userId)`
- **Index Required**: Default single-field index is sufficient.

## Collection: `breedingScenarios`

### Query: User Scenarios
- **Usage**: `BreedingScenarioRepository.getMyScenarios(userId)`
- **Query**: `.whereEqualTo("ownerUserId", userId)`
- **Index Required**: Default single-field index is sufficient.

## Collection: `breedStandards`

### Query: Breeds by Species
- **Usage**: `BreedRepository.getBreedsForSpecies(speciesId)`
- **Query**: `.whereEqualTo("species", speciesId)`
- **Index Required**: Default single-field index is sufficient.

---

## Security Rule Consistency Check

### `tickets` Collection
- **Rule**: `allow list: if request.query.limit <= 500 && request.auth.uid == resource.data.userId` (Implicitly enforced by query filters matching rules)
- **Consistency**: `SupportRepository` filters by `userId`, which aligns with the owner-only read rule. Admins can read all, which is also supported.

### `breedingSimulations` & `breedingScenarios`
- **Rule**: Owner access only (`resource.data.ownerId == request.auth.uid`).
- **Consistency**: App queries always filter by `ownerId`/`ownerUserId`, ensuring successful rule evaluation.
