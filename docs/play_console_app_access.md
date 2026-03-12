# Google Play Console - App Access

To facilitate Google Play App Review, we provide a dedicated reviewer account that mimics a PRO subscription via a Firestore configuration overlay.

## Credentials
**Username:** `test@hatchtracker.com`
**Password:** `Test123!`

**Note:** This account has no 2FA and no email verification required.

## Technical Implementation
This account does **not** have a real Google Play Billing subscription.
Instead, the app checks `config/app_access` in Firestore.

If the signed-in email matches an entry in `reviewAccounts` AND `reviewEnabled` is true, the app grants the configured `reviewTier` (default: PRO).

## Config Location (Firestore)
Path: `config/app_access`

Fields:
- `reviewEnabled`: boolean
- `reviewAccounts`: array<string> (e.g. `["test@hatchtracker.com"]`)
- `reviewTier`: string (e.g. `PRO`)
