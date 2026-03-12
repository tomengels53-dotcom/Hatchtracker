package com.example.hatchtracker.app

import android.app.Application
import android.content.pm.ApplicationInfo
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
open class HatchApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var billingManager: dagger.Lazy<com.example.hatchtracker.billing.BillingManager>

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var integrityDebugRunner: dagger.Lazy<com.example.hatchtracker.data.validation.IntegrityDebugRunner>

    /**
     * Structured scope for application-level background tasks.
     * Tied to the application lifecycle.
     */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        if (com.example.hatchtracker.BuildConfig.DEBUG) {
            android.os.StrictMode.setThreadPolicy(
                android.os.StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath() // Enforce P95 targets by crashing on violations
                    .build()
            )
            android.os.StrictMode.setVmPolicy(
                android.os.StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
        
        androidx.tracing.Trace.beginSection("HatchApplication.onCreate")
        super.onCreate()
        
        val isBenchmark = com.example.hatchtracker.BuildConfig.BUILD_TYPE == "benchmark"
        if (isBenchmark) {
            androidx.tracing.Trace.endSection()
            return
        }

        // 1. Explicitly initialize Firebase first
        androidx.tracing.Trace.beginSection("Firebase.initializeApp")
        FirebaseApp.initializeApp(this)
        androidx.tracing.Trace.endSection()

        // 2. Install Firebase App Check immediately (before Firestore/Functions/Billing)
        if (com.example.hatchtracker.BuildConfig.DEBUG) {
            androidx.tracing.Trace.beginSection("AppCheck.DebugInit")
            try {
                val debugFactoryClass = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                val getInstanceMethod = debugFactoryClass.getMethod("getInstance")
                val factory = getInstanceMethod.invoke(null) as com.google.firebase.appcheck.AppCheckProviderFactory
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)
                android.util.Log.i("AppCheck", "Installed DebugAppCheckProviderFactory (debug build)")
            } catch (e: Exception) {
                android.util.Log.e("AppCheck", "Could not initialize DebugAppCheckProviderFactory", e)
            } finally {
                androidx.tracing.Trace.endSection()
            }
        } else {
            androidx.tracing.Trace.beginSection("AppCheck.PlayIntegrityInit")
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            android.util.Log.i("AppCheck", "Installed PlayIntegrityAppCheckProviderFactory (release/staging build)")
            androidx.tracing.Trace.endSection()
        }
        
        // Initialize Notification Channel
        androidx.tracing.Trace.beginSection("NotificationHelper.createChannel")
        com.example.hatchtracker.NotificationHelper.createNotificationChannel(this)
        androidx.tracing.Trace.endSection()
        
        // Initialize Firebase Crashlytics and Analytics collection based on build config
        val enableCrashlytics =
            com.example.hatchtracker.BuildConfig.ENABLE_CRASHLYTICS &&
                !com.example.hatchtracker.BuildConfig.DEBUG
        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enableCrashlytics)
        com.google.firebase.analytics.FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(enableCrashlytics)

        if (enableCrashlytics) {
            val crashlytics = com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("build_env", com.example.hatchtracker.BuildConfig.FIREBASE_ENV)
            // Default "app_init", can be updated during navigation
            crashlytics.setCustomKey("module", "app_init")
            // Default "unknown", can be updated after user logs in
            crashlytics.setCustomKey("tier", "unknown")
        }

        // Defer non-critical initializations to background coroutine to speed up `onCreate`
        applicationScope.launch(Dispatchers.IO) {
            // Schedule periodic workers. These are Idempotent (enqueueUniquePeriodicWork).
            // Scheduled here in background to prevent splash screen stutters or ANR risks.
            com.example.hatchtracker.NotificationHelper.schedulePeriodicChecks(this@HatchApplication)
            com.example.hatchtracker.NotificationHelper.scheduleHatchyReminders(this@HatchApplication)

            // Start billing connection to reconcile subscriptions with Firestore
            billingManager.get().startConnection()

            // Schedule Cost Basis Engine offline allocations
            com.example.hatchtracker.data.worker.CostAccountingWorker.schedule(this@HatchApplication)

            // Prune completed WorkManager tasks to prevent DB bloat
            androidx.work.WorkManager.getInstance(this@HatchApplication).pruneWork()

            // Trigger Data Integrity Validator (Debug builds only check inside runner)
            integrityDebugRunner.get().onAppStartup()
        }

        androidx.tracing.Trace.endSection() // End HatchApplication.onCreate
    }
}
