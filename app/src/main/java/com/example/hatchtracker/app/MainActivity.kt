package com.example.hatchtracker.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.core.ui.R
import com.example.hatchtracker.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.hatchtracker.feature.mainmenu.navigation.HatchBaseNavHost
import com.example.hatchtracker.core.navigation.NavRoute
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @javax.inject.Inject
    lateinit var subscriptionStateManager: com.example.hatchtracker.billing.SubscriptionStateManager

    @javax.inject.Inject
    lateinit var adsManager: com.example.hatchtracker.ads.AdsManager

    @javax.inject.Inject
    lateinit var userRepository: com.example.hatchtracker.data.repository.UserRepository

    @javax.inject.Inject
    lateinit var authClaimsRepository: com.example.hatchtracker.data.repository.AuthClaimsRepository

    @javax.inject.Inject
    lateinit var rewardManager: com.example.hatchtracker.ads.RewardManager

    @javax.inject.Inject
    lateinit var billingManager: com.example.hatchtracker.billing.BillingManager




    @javax.inject.Inject
    lateinit var sessionManager: com.example.hatchtracker.auth.SessionManager

    @javax.inject.Inject
    lateinit var onboardingManager: com.example.hatchtracker.core.common.OnboardingManager

    @javax.inject.Inject
    lateinit var hatchyContextProvider: com.example.hatchtracker.core.navigation.HatchyContextProvider

    @javax.inject.Inject
    lateinit var themeRepository: com.example.hatchtracker.data.theme.ThemeRepository

    @javax.inject.Inject
    lateinit var database: com.example.hatchtracker.data.AppDatabase

    @javax.inject.Inject
    lateinit var languagePreferences: com.example.hatchtracker.common.localization.LanguagePreferences

    @javax.inject.Inject
    lateinit var localeManager: com.example.hatchtracker.common.localization.LocaleManager

    @javax.inject.Inject
    lateinit var fcmTokenRepository: com.example.hatchtracker.notifications.push.FcmTokenRepository

    @javax.inject.Inject
    lateinit var configRepository: com.example.hatchtracker.data.repository.ConfigRepository

    private lateinit var jankStats: androidx.metrics.performance.JankStats
    private lateinit var frameMetricsAggregator: androidx.core.app.FrameMetricsAggregator

    override fun onCreate(savedInstanceState: Bundle?) {
        androidx.tracing.Trace.beginSection("MainActivity.onCreate")
        val isBenchmark = com.example.hatchtracker.BuildConfig.BUILD_TYPE == "benchmark"
        
        if (isBenchmark) {
            setTheme(com.example.hatchtracker.R.style.Theme_HatchBase)
        }
        
        if (!isBenchmark) {
            installSplashScreen()
            
            androidx.tracing.Trace.beginSection("MainActivity.NotificationsInit")
            frameMetricsAggregator = androidx.core.app.FrameMetricsAggregator(androidx.core.app.FrameMetricsAggregator.TOTAL_DURATION)
            frameMetricsAggregator.add(this)

            // 2. Initialize JankStats
            val jankFrameListener = androidx.metrics.performance.JankStats.OnFrameListener { frameData ->
                if (frameData.isJank) {
                    com.example.hatchtracker.core.logging.Logger.w(
                        LogTags.PERFORMANCE, 
                        "Jank detected! State: ${frameData.states}, Duration: ${frameData.frameDurationUiNanos / 1_000_000}ms"
                    )
                }
            }
            jankStats = androidx.metrics.performance.JankStats.createAndTrack(window, jankFrameListener)
            jankStats.isTrackingEnabled = true
        }

        super.onCreate(savedInstanceState)
        
        if (!isBenchmark) {
            androidx.tracing.Trace.beginSection("MainActivity.NotificationsInit")
            // Create notification channel for hatch alerts
            try {
                NotificationHelper.createNotificationChannel(this)
            } catch (e: Exception) {
                Logger.e(LogTags.NOTIFICATIONS, "Failed to init notifications: ${e.message}", e)
            }
            androidx.tracing.Trace.endSection()

            // Defer non-critical startup work (ads init, FCM token sync) to a background
            // coroutine. This keeps them off the main thread and off the critical path to
            // first frame, without blocking setContent.
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                try {
                    androidx.tracing.Trace.beginSection("MainActivity.AdsInit")
                    adsManager.initialize()
                } catch (e: Exception) {
                    Logger.e(LogTags.BILLING, "Failed to init ads: ${e.message}", e)
                } finally {
                    androidx.tracing.Trace.endSection()
                }
                fcmTokenRepository.requestTokenSyncAsync()
            }
        }
        
        enableEdgeToEdge()
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        androidx.tracing.Trace.beginSection("MainActivity.setContent")
        setContent {
            androidx.compose.runtime.SideEffect {
                // This will run after the first successful composition
                androidx.tracing.Trace.endSection() // End MainActivity.setContent
                androidx.tracing.Trace.endSection() // End MainActivity.onCreate
                android.util.Log.i("Performance", "MainActivity: First composition finished")
            }
            val themeMode by themeRepository.themeMode.collectAsState(initial = com.example.hatchtracker.domain.theme.ThemeMode.LIGHT)
            val isDarkTheme = when (themeMode) {
                com.example.hatchtracker.domain.theme.ThemeMode.LIGHT -> false
                com.example.hatchtracker.domain.theme.ThemeMode.DARK -> true
            }

            com.example.hatchtracker.app.theme.HatchBaseTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val context = LocalContext.current
                var dbError by remember { mutableStateOf<String?>(null) }

                if (dbError != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.error_critical_data, dbError ?: ""),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    val catalogRepository: com.example.hatchtracker.data.repository.CatalogRepository = remember(database) { com.example.hatchtracker.data.repository.CatalogRepository(database) }

                    LaunchedEffect(Unit) {
                        try {
                            catalogRepository.seedCatalogIfEmpty()
                        } catch (e: Exception) {
                            Logger.e(LogTags.DB, "Catalog Seeding Init Failed: ${e.message}", e)
                            dbError = e.message ?: context.getString(R.string.error_game_data_init)
                        }
                    }

                    val sessionState by sessionManager.sessionState.collectAsState()
                    val user = (sessionState as? com.example.hatchtracker.auth.SessionState.Authenticated)?.user
                    val startDestination = NavRoute.Welcome.route

                    LaunchedEffect(user) {
                        com.example.hatchtracker.core.logging.CrashContextLogger.setUser(user?.uid)
                        fcmTokenRepository.onUserAuthenticated(user?.uid)
                    }

                    val profile by if (user != null) {
                        userRepository.getProfileFlow(user.uid).collectAsState(initial = null)
                    } else {
                        remember { mutableStateOf(null) }
                    }

                    LaunchedEffect(user, profile) {
                        if (user != null && profile == null) {
                            if (!com.example.hatchtracker.BuildConfig.BUILD_TYPE.equals("benchmark")) {
                                delay(2000)
                            }
                            if (userRepository.userProfile.value == null) {
                                userRepository.ensureProfileFromAuth(user)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize().semantics { contentDescription = "app_root" }.testTag("app_root"),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val isBenchmark = com.example.hatchtracker.BuildConfig.BUILD_TYPE == "benchmark"
                        var showSplash by remember { mutableStateOf(!isBenchmark) }
                        
                        if (isBenchmark) {
                            SideEffect {
                                reportFullyDrawn()
                            }
                        }
                        
                        if (showSplash) {
                            LaunchedEffect(Unit) {
                                delay(1200)
                                showSplash = false
                            }
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                HatchBaseNavHost(
                                    navController = navController,
                                    subscriptionStateManager = subscriptionStateManager,
                                    authClaimsRepository = authClaimsRepository,
                                    configRepository = configRepository,
                                    sessionState = sessionState,
                                    user = user, // Deprecated param, keeping for graph compat temporarily
                                    profile = profile,
                                    onboardingManager = onboardingManager,
                                    startDestination = startDestination,
                                    sessionManager = sessionManager,
                                    userRepository = userRepository,
                                    themeRepository = themeRepository,
                                    languagePreferences = languagePreferences,
                                    localeManager = localeManager
                                )
    
                                val navBackStackEntry by navController.currentBackStackEntryAsState()
                                val currentRoute = navBackStackEntry?.destination?.route

                                LaunchedEffect(navBackStackEntry) {
                                    val route = navBackStackEntry?.destination?.route
                                    if (route != null) {
                                        val capabilities = subscriptionStateManager.currentCapabilities.value
                                        val profile = userRepository.userProfile.value
                                        val args = navBackStackEntry?.arguments
                                        
                                        com.example.hatchtracker.core.logging.CrashContextLogger.setCurrentRoute(route)
                                        com.example.hatchtracker.core.logging.CrashContextLogger.setSubscriptionTier(capabilities.tier.name)
                                        
                                        // God-Mode Target: Reset and check Firebase read budget constraint per navigation
                                        com.example.hatchtracker.core.logging.FirebasePerfTracer.resetNavigationCounters()

                                        hatchyContextProvider.updateContext(
                                            route = route,
                                            tier = capabilities.tier,
                                            isAdmin = profile?.isSystemAdmin == true || profile?.isDeveloper == true,
                                            species = args?.getString("species"),
                                            entityId = args?.getString("flockId") ?: args?.getString("incubationId") ?: args?.getString("birdId")
                                        )
                                    }
                                }
    
                                val showHatchy = currentRoute == NavRoute.MainMenu.createRoute("home")
    
                                if (showHatchy) {
                                    com.example.hatchtracker.core.ui.components.HatchyAccessibilityIcon(
                                        onClick = { navController.navigate(NavRoute.HatchyChat.route) },
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(bottom = 96.dp, start = 16.dp)
                                    )
                                }

                                if (com.example.hatchtracker.BuildConfig.DEBUG) {
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::jankStats.isInitialized) {
            jankStats.isTrackingEnabled = true
        }
    }

    override fun onPause() {
        super.onPause()
        if (::jankStats.isInitialized) {
            jankStats.isTrackingEnabled = false
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::frameMetricsAggregator.isInitialized) {
            frameMetricsAggregator.remove(this)
        }
    }
}
