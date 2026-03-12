# HatchTracker Observability & Monitoring

This document provides operational guidance for monitoring the health, stability, and usage of the HatchTracker application in production and staging environments.

## Where to Monitor

### 1. Application Not Responding (ANRs)
- **Tool**: Google Play Console -> Android Vitals.
- **Why**: Google Play measures the core health of the app (crash rate and ANR rate). Staying below the bad behavior threshold is critical for store visibility.
- **Action**: Monitor the ANR rate. ANRs are typically caused by blocking the main thread (e.g., heavy database migrations, file I/O, synchronous network calls). Use the stack traces provided by Play Console to identify the blocking method and move it to `Dispatchers.IO`.

### 2. Crash Reporting
- **Tool**: Firebase Crashlytics.
- **Overview**: Captures fatal crashes and non-fatal exceptions.
- **Environment Targeting**:
  - `releaseStaging` and `release` build types both report to the single Firebase project using `app/google-services.json`. 
  - To differentiate staging vs. production crashes, filter the Crashlytics dashboard using the custom key: `build_env = staging` or `build_env = prod`.
  - Development crashes (`debug` builds) do not pollute the dashboard, as collection is disabled via `BuildConfig.ENABLE_CRASHLYTICS`.
- **Custom Keys**: The following keys are attached to crash reports to aid debugging:
  - `build_env` (String): The build environment (`prod`, `staging`, `debug`).
  - `module` (String): The current functional module the user was in (defaults to `app_init`).
  - `tier` (String): The user's subscription tier.

### 3. User Retention & Engagement
- **Tool**: Firebase Analytics (Google Analytics 4).
- **Why**: To measure how often users return and which features keep them engaged.
- **Recommended Events for Retention Tracking**:
  To get meaningful funnel and retention data in GA4, the following events should be systematically logged across the app lifecycle:
  - `completed_onboarding`: User completes the initial setup or tutorial.
  - `created_first_flock`: Significant activation metric.
  - `logged_egg_production`: Core daily sticky action.
  - `created_incubation`: Core weekly/monthly sticky action.
  - `viewed_financial_stats`: High-value feature indicator.
  - `created_breeding_scenario`: Premium engagement indicator.
  - `opened_paywall`: Intent to subscribe.
  - `started_trial`: Conversion metric.
  - `subscribed`: Realized revenue metric.

## Using the Test Crash
If you need to verify that crashes are uploading properly to Firebase:
1. Compile a `debug` build and deploy it to a device.
2. Navigate to **Developer Diagnostics** (Admin panel).
3. Tap **Trigger Test Crash**.
4. *Note: Since collection is disabled in `debug` by default, this will only prove the button works locally. To test actual uploading, you must temporarily set `ENABLE_CRASHLYTICS = true` for debug, or use a `releaseStaging` build.*

## Note on Firebase Staging Environment
Currently, the application uses a single `app/google-services.json` file for all builds. Production and staging metrics flow into the same Firebase project. 
- *Trade-off*: You must rely on filtering by `build_env` to isolate staging data from production analytics.
- *Future Improvement*: Add variant-specific configs (e.g., `app/src/releaseStaging/google-services.json`) pointing to a secondary Firebase project dedicated to staging.
