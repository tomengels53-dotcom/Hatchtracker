# Transactional Email Lifecycle

This document explains the technical flow and operational governance of transactional emails in the HatchTracker platform.

## 1. Delivery Architecture
We utilize the **Firebase "Trigger Email" Extension** as our delivery bridge.
- **Trigger**: A Cloud Function writes a document to the `mail` collection.
- **Transmission**: The Extension detects the new document and transmits the payload via SendGrid/SMTP.
- **Feedback**: The Extension updates the `mail` document status (`SUCCESS` or `ERROR`).

## 2. Event Workflow
1. **Application Hook**: A lifecycle event occurs (e.g., `verifyPlaySubscription` succeeds).
2. **Audit & Preparation**:
   - Create a `pending` record in `emailEvents/{id}`.
   - Assemble the HTML payload and create a document in `mail/{id}`.
3. **Transmission**: Extension handles the external API call (SendGrid).
4. **Finalization**:
   - A background Cloud Function (future) or manual audit reconciles `mail` status back to `emailEvents`.

## 3. Email Types & Triggers
| Event | Type | Trigger |
|-------|------|---------|
| Successful Sub | `purchase_confirmation` | `verifyPlaySubscription` (on success) |
| Invoice Generated | `invoice` | Post-payment reconciliation |
| Renewal Warning | `renewal_reminder` | Scheduled task (3 days before expiry) |
| Expiration | `expiration` | Subscription expiry check |

## 4. Compliance & Privacy
- **GDPR**: Subscription emails are considered "strictly necessary for the performance of a contract." They do not require an unsubscribe link, though we include one for support.
- **Retention**: Records in `emailEvents` are retained for the duration of the subscription plus 12 months for support/audit purposes.
- **No Marketing**: Our server-side logic strictly prevents cross-contaminating transactional triggers with marketing segments.
