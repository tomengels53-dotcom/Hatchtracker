# Admin security Model

This document outlines the security architecture for the HatchTracker Admin Panel, ensuring that sensitive management actions are strictly protected.

## 1. Authorization Hierarchy
Access is controlled via **Firebase Custom Claims**, which are immutable on the client and verified on every server-side request.

- **`isAdmin: true`**: Full access to dashboard, financial data (VAT/Invoices), and role management.
- **`isModerator: true`**: Access to content moderation tools (Reports, Posts) but restricted from financial or sensitive user data.

## 2. Claim Provisioning
Custom claims cannot be set via the client. They are provisioned through:
1. **Initial Setup**: A one-time server script using the Admin SDK.
2. **Management**: Existing Admins calling `adminUpdateUserRole` (Cloud Function).

## 3. Communication Bridge
The React frontend never uses `set()`, `update()`, or `delete()` on high-privilege collections.

- **Reads**: Performed via the standard Firebase JS SDK, filtered by `firestore.rules` (which check the `auth.token.isAdmin` claim).
- **Writes**: Performed exclusively via **HTTPS Callable Functions**. These functions:
    - Verify the caller's identity.
    - Validate the `isAdmin` claim.
    - Perform the operation using the **Firebase Admin SDK**.

## 4. UI Safety Principles
- **Read-Only Default**: All data is displayed in a non-editable state.
- **Action Gateways**: Destructive actions (e.g., revoking Pro access) are hidden behind a secondary confirmation dialog.
- **Visibility**: The `isAdmin` claim is used to conditionally render management UI elements to prevent unauthorized interface access.
