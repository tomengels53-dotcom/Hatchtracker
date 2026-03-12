@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.feature.mainmenu.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.hatchtracker.auth.SessionState
import com.example.hatchtracker.billing.SubscriptionStateManager
import com.example.hatchtracker.core.common.OnboardingManager
import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
// FeatureEntry import removed
import com.example.hatchtracker.core.navigation.NavRoute
import com.example.hatchtracker.core.navigation.BreedSelectionContract
import com.example.hatchtracker.core.navigation.ScannerContracts
import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.SubscriptionTier
import com.example.hatchtracker.data.repository.AuthClaimsRepository
import com.example.hatchtracker.domain.model.UserProfile
import com.example.hatchtracker.data.repository.UserRepository
import com.example.hatchtracker.feature.mainmenu.screens.MainMenuScreen
import com.example.hatchtracker.feature.mainmenu.screens.OnboardingScreen
import com.example.hatchtracker.feature.mainmenu.screens.SettingsDrawerContent
import com.example.hatchtracker.feature.mainmenu.BuildConfig
import com.example.hatchtracker.feature.admin.AdminAuditLogScreen
import com.example.hatchtracker.feature.admin.AdminMenuScreen
import com.example.hatchtracker.feature.admin.AdminTicketDashboardScreen
import com.example.hatchtracker.feature.admin.AdminTicketDetailView
import com.example.hatchtracker.feature.admin.TicketAction
import com.example.hatchtracker.feature.admin.BreedAdminScreen
import com.example.hatchtracker.feature.breeding.TraitPromotionScreen
import com.example.hatchtracker.feature.auth.LoginScreen
import com.example.hatchtracker.feature.auth.SignUpScreen
import com.example.hatchtracker.feature.auth.WelcomeScreen
import com.example.hatchtracker.feature.auth.LegalScreen
import com.example.hatchtracker.feature.auth.LegalDocumentType
import com.example.hatchtracker.feature.bird.AddBirdScreen
import com.example.hatchtracker.feature.bird.BirdDetailScreen
import com.example.hatchtracker.feature.bird.BirdListScreen
import com.example.hatchtracker.feature.breeding.BreedingHistoryScreen
import com.example.hatchtracker.feature.breeding.BreedingScreen
import com.example.hatchtracker.feature.breeding.TraitObservationScreen
import com.example.hatchtracker.feature.devices.AddDeviceScreen
import com.example.hatchtracker.feature.incubation.TroubleshootingScreen
import com.example.hatchtracker.feature.incubation.TroubleshootingViewModel
import com.example.hatchtracker.feature.finance.AddFinancialEntryScreen
import com.example.hatchtracker.feature.finance.AddSalesBatchScreen
import com.example.hatchtracker.feature.finance.FinancialStatsScreen
import com.example.hatchtracker.feature.finance.FinancialTransactionsScreen
import com.example.hatchtracker.feature.finance.PaywallScreen
import com.example.hatchtracker.feature.flock.ui.screens.AddFlockScreen
import com.example.hatchtracker.feature.flock.ui.screens.FlockDetailScreen
import com.example.hatchtracker.feature.flock.ui.screens.FlockListScreen
import com.example.hatchtracker.feature.incubation.AddIncubationScreen
import com.example.hatchtracker.feature.incubation.HatchOutcomeScreen
import com.example.hatchtracker.feature.incubation.HatchPlannerScreen
import com.example.hatchtracker.feature.incubation.IncubationDetailScreen
import com.example.hatchtracker.feature.incubation.IncubationListScreen
import com.example.hatchtracker.feature.nursery.NurseryScreen
import com.example.hatchtracker.feature.production.EggProductionScreen
import com.example.hatchtracker.feature.profile.UserProfileScreen
import com.example.hatchtracker.feature.profile.ProfileSetupScreen // Added
import com.example.hatchtracker.feature.support.HelpSupportScreen
import com.example.hatchtracker.feature.community.moderation.ui.*
import kotlinx.coroutines.launch

import com.example.hatchtracker.common.localization.LanguagePreferences
import com.example.hatchtracker.common.localization.LocaleManager

@Composable
fun HatchBaseNavHost(
    navController: NavHostController,
    subscriptionStateManager: SubscriptionStateManager,
    authClaimsRepository: AuthClaimsRepository,
    configRepository: com.example.hatchtracker.data.repository.ConfigRepository,
    sessionState: SessionState,
    user: com.google.firebase.auth.FirebaseUser?,
    profile: UserProfile?,
    onboardingManager: OnboardingManager,
    startDestination: String,
    sessionManager: com.example.hatchtracker.auth.SessionManager,
    userRepository: UserRepository,
    themeRepository: com.example.hatchtracker.data.theme.ThemeRepository,
    languagePreferences: LanguagePreferences,
    localeManager: LocaleManager
){
    val isAdmin by subscriptionStateManager.isAdmin.collectAsState()
    val isDeveloper by subscriptionStateManager.isDeveloper.collectAsState()
    val isCommunityAdmin by subscriptionStateManager.isCommunityAdmin.collectAsState()

    // Navigation is centralized. Do not introduce FeatureEntry-based navigation.
    NavHost(navController = navController, startDestination = startDestination) {
        authGraph(navController, onboardingManager, sessionState, userRepository)
        mainGraph(
            navController, 
            subscriptionStateManager, 
            authClaimsRepository,
            configRepository,
            user, 
            profile, 
            sessionManager, 
            themeRepository, 
            languagePreferences, 
            localeManager, 
            userRepository
        )
        flockGraph(navController)
        birdGraph(navController)
        incubationGraph(navController)
        nurseryGraph(navController)
        financialGraph(navController, profile, subscriptionStateManager)
        breedingGraph(navController, profile, subscriptionStateManager)
        adminGraph(navController, profile, subscriptionStateManager, authClaimsRepository)
        breedSelectionGraph(navController)
        notificationsGraph(navController)
        supportGraph(navController)
        scannerGraph(navController)
        moderationGraph(
            navController = navController,
            isCommunityAdmin = isCommunityAdmin,
            isSystemAdmin = isAdmin
        )

        composable(NavRoute.HatchyChat.route) {
            com.example.hatchtracker.feature.support.HatchyChatScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = NavRoute.Legal.route,
            arguments = listOf(navArgument("docType") { type = NavType.StringType })
        ) { backStackEntry ->
            val docType = backStackEntry.arguments?.getString("docType") ?: "privacy"
            val type = if (docType == "terms") LegalDocumentType.TERMS_OF_SERVICE else LegalDocumentType.PRIVACY_POLICY
            LegalScreen(type = type, onBackClick = { navController.popBackStack() })
        }

        composable(NavRoute.CommunityDevTools.route) {
            if (isDeveloper || isAdmin) {
                com.example.hatchtracker.feature.community.devtools.ui.CommunityDevToolsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                // Unauthorized access to internal tools
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
    }

    // Centralized Navigation Guard
    NavigationGuard(
        navController = navController,
        sessionState = sessionState,
        profile = profile,
        subscriptionStateManager = subscriptionStateManager
    )
}

fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    onboardingManager: OnboardingManager,
    sessionState: SessionState,
    userRepository: UserRepository
) {
    composable(NavRoute.Welcome.route) {
        WelcomeScreen(
            onContinue = {
                onboardingManager.setSeenWelcome()
                if (sessionState is SessionState.Authenticated) {
                    val nextRoute = if (onboardingManager.hasCompletedOnboarding()) {
                        NavRoute.MainMenu.createRoute("home")
                    } else {
                        NavRoute.Onboarding.route
                    }
                    navController.navigate(nextRoute) {
                        popUpTo(NavRoute.Welcome.route) { inclusive = true }
                    }
                } else {
                    navController.navigate(NavRoute.Login.route) {
                        popUpTo(NavRoute.Welcome.route) { inclusive = true }
                    }
                }
            }
        )
    }

    composable(NavRoute.Login.route) {
        val isAuthenticated = sessionState is SessionState.Authenticated
        LoginScreen(
            isAuthenticated = isAuthenticated,
            onLoginSuccess = {
                val nextRoute = if (onboardingManager.hasCompletedOnboarding()) {
                    NavRoute.MainMenu.createRoute("home")
                } else {
                    NavRoute.Onboarding.route
                }
                navController.navigate(nextRoute) {
                    popUpTo(NavRoute.Login.route) { inclusive = true }
                }
            },
            onNavigateToSignUp = {
                navController.navigate(NavRoute.SignUp.route)
            }
        )
    }

    composable(NavRoute.SignUp.route) {
        SignUpScreen(
            onSignUpSuccess = {
                // New users always go to onboarding
                navController.navigate(NavRoute.Onboarding.route) {
                    popUpTo(NavRoute.Welcome.route) { inclusive = true }
                }
            },
            onNavigateToLogin = {
                navController.popBackStack()
            }
        )
    }

    composable(NavRoute.Onboarding.route) {
        OnboardingScreen(
            onComplete = { nextRoute ->
                onboardingManager.setCompletedOnboarding()
                navController.navigate(nextRoute) {
                    popUpTo(NavRoute.Onboarding.route) { inclusive = true }
                }
            },
            onSkip = {
                onboardingManager.setCompletedOnboarding()
                navController.navigate(NavRoute.MainMenu.createRoute("home")) {
                    popUpTo(NavRoute.Onboarding.route) { inclusive = true }
                }
            }
        )
    }
}

fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    subscriptionStateManager: SubscriptionStateManager,
    authClaimsRepository: AuthClaimsRepository,
    configRepository: com.example.hatchtracker.data.repository.ConfigRepository,
    user: com.google.firebase.auth.FirebaseUser?,
    profile: UserProfile?,
    sessionManager: com.example.hatchtracker.auth.SessionManager,
    themeRepository: com.example.hatchtracker.data.theme.ThemeRepository,
    languagePreferences: LanguagePreferences,
    localeManager: LocaleManager,
    userRepository: UserRepository
) {
    composable(
        route = NavRoute.MainMenu.route,
    ) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.8f)) {
                    SettingsDrawerContent(
                        subscriptionStateManager = subscriptionStateManager,
                        authClaimsRepository = authClaimsRepository,
                        configRepository = configRepository,
                        themeRepository = themeRepository,
                        languagePreferences = languagePreferences,
                        localeManager = localeManager,
                        onClose = { scope.launch { drawerState.close() } },
                        onSignOut = {
                            sessionManager.signOut()
                        },
                        onNavigateToProfile = {
                            scope.launch { drawerState.close() }
                            navController.navigate(NavRoute.UserProfile.route)
                        },
                        onNavigateToSupport = {
                            scope.launch { drawerState.close() }
                            navController.navigate(NavRoute.HelpSupport.route)
                        },
                        onNavigateToDevTools = {
                            scope.launch { drawerState.close() }
                            navController.navigate(NavRoute.CommunityDevTools.route)
                        }
                    )
                }
            },
            gesturesEnabled = drawerState.isOpen
        ) {
            MainMenuScreen(
                navController = navController,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onNavigateToPaywall = { navController.navigate(NavRoute.Paywall.route) },
                onNavigateToAdmin = { navController.navigate(NavRoute.AdminMenu.route) },
                onNavigateToFlock = { navController.navigate(NavRoute.FlockList.route) },
                onNavigateToIncubation = { navController.navigate(NavRoute.IncubationList.route) },
                onNavigateToBreeding = { navController.navigate(NavRoute.Breeding.route) },
                onNavigateToNursery = { navController.navigate(NavRoute.Nursery.route) },
                onNavigateToFinance = { navController.navigate(NavRoute.FinancialStats.route) },
                onNavigateToProduction = { navController.navigate(NavRoute.EggProduction.createRoute()) }
            )
        }
    }

    composable(NavRoute.Paywall.route) {
        PaywallScreen(
            onDismiss = { navController.popBackStack() }
        )
    }

    composable(NavRoute.UserProfile.route) {
        UserProfileScreen(
            onBack = { navController.popBackStack() },
            onNavigateToAddDevice = { navController.navigate(NavRoute.AddDevice.route) },
            onNavigateToSupport = {
                navController.navigate(NavRoute.HelpSupport.createRoute("user_profile", "change_country"))
            },
            onNavigateToPaywall = { navController.navigate(NavRoute.Paywall.route) },
            onNavigateToProfileSetup = { navController.navigate(NavRoute.ProfileSetup.route) } // Added
        )
    }

    composable(NavRoute.ProfileSetup.route) { // Added
        val scope = rememberCoroutineScope()
        val userProfile by userRepository.userProfile.collectAsState()
        
        ProfileSetupScreen(
            initialCountryCode = userProfile?.countryCode,
            onComplete = { country, weightUnit, dateFormat, timeFormat, currencyCode ->
                scope.launch {
                   userProfile?.let { current ->
                        val updated = current.copy(
                            countryCode = country,
                            weightUnit = weightUnit,
                            dateFormat = dateFormat,
                            timeFormat = timeFormat,
                            currencyCode = currencyCode
                        )
                        userRepository.updateProfile(updated)
                   }
                   navController.popBackStack()
                }
            }
        )
    }

    composable(
        NavRoute.HelpSupport.route,
        arguments = listOf(
            navArgument("moduleId") { type = NavType.StringType; nullable = true; defaultValue = null },
            navArgument("featureId") { type = NavType.StringType; nullable = true; defaultValue = null }
        )
    ) { backStackEntry ->
        val moduleId = backStackEntry.arguments?.getString("moduleId")
        val featureId = backStackEntry.arguments?.getString("featureId")

        HelpSupportScreen(
            onBack = { navController.popBackStack() },
            initialModuleId = moduleId,
            initialFeatureId = featureId,
            onNavigateToAdminDashboard = {
                navController.navigate(NavRoute.AdminTicketDashboard.route)
            }
        )
    }

    composable(NavRoute.AddDevice.route) {
        AddDeviceScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

}

fun NavGraphBuilder.flockGraph(navController: NavHostController) {
    composable(NavRoute.FlockList.route) {
        FlockListScreen(
            onBackClick = { navController.popBackStack() },
            onFlockClick = { flockId ->
                navController.navigate(NavRoute.FlockDetail.createRoute(flockId))
            },
            onAddFlockClick = { navController.navigate(NavRoute.AddFlock.createRoute()) },
            onAddFinancialEntry = { ownerId, ownerType, isRevenue ->
                navController.navigate(NavRoute.AddFinancialEntry.createRoute(ownerId, ownerType, isRevenue))
            },
            onRecordSale = { ownerId, ownerType ->
                navController.navigate(NavRoute.AddSalesBatch.createRoute(ownerId, ownerType))
            },
            onNavigateToEquipment = { navController.navigate(NavRoute.AddDevice.route) }
        )
    }

    composable(
        route = NavRoute.AddFlock.route,
        arguments = listOf(
            navArgument("species") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("prefilledBreeds") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("graduatingFlockletId") {
                type = NavType.LongType
                defaultValue = -1L
            }
        )
    ) { backStackEntry ->
        val species = backStackEntry.arguments?.getString("species")
        val breedsStr = backStackEntry.arguments?.getString("prefilledBreeds")
        val breeds = breedsStr?.split(",")?.filter { it.isNotBlank() }
        val flockletIdArg = backStackEntry.arguments?.getLong("graduatingFlockletId") ?: -1L
        val flockletId = if (flockletIdArg == -1L) null else flockletIdArg

        val breedName by backStackEntry.savedStateHandle.getLiveData<String>(BreedSelectionContract.KEY_BREED_NAME).observeAsState()
        var selectedBreed by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(breedName) {
            if (breedName != null) {
                val result = BreedSelectionContract.consumeSelectionResult(navController)
                if (result != null) {
                    selectedBreed = result.breedName
                }
            }
        }

        AddFlockScreen(
            onBackClick = { navController.popBackStack() },
            onFlockSaved = { navController.popBackStack() },
            prefilledSpecies = species,
            prefilledBreeds = breeds,
            graduatingFlockletId = flockletId,
            onNavigateToBreedSelection = { speciesId ->
                navController.navigate(NavRoute.BreedSelection.createRoute(speciesId))
            },
            selectedBreedFromResult = selectedBreed,
            onClearBreedResult = { selectedBreed = null }
        )
    }

    composable(
        route = NavRoute.FlockDetail.route,
        arguments = listOf(navArgument("flockId") { type = NavType.LongType })
    ) { backStackEntry ->
        val viewModel: com.example.hatchtracker.feature.flock.ui.viewmodels.FlockDetailViewModel = hiltViewModel()

        FlockDetailScreen(
            viewModel = viewModel,
            onBackClick = { navController.popBackStack() },
            onAddBirdClick = { fId, species ->
                navController.navigate(NavRoute.AddBird.createRoute(flockId = fId, species = species))
            },
            onStartIncubationClick = { fId, species ->
                navController.navigate(NavRoute.AddIncubation.createRoute(flockId = fId, species = species))
            },
            onRecordSale = { ownerId, ownerType ->
                navController.navigate(NavRoute.AddSalesBatch.createRoute(ownerId, ownerType))
            },
            onSellBirds = { ownerId, ownerType, birdIds ->
                navController.navigate(NavRoute.AddSalesBatch.createRoute(ownerId, ownerType, birdIds))
            },
            onAddFinancialEntry = { oId, oType, isRev ->
                navController.navigate(NavRoute.AddFinancialEntry.createRoute(oId, oType, isRev))
            },
            onLogProduction = { flockId ->
                navController.navigate(NavRoute.EggProduction.createRoute(flockId))
            },
            onBirdClick = { birdId ->
                navController.navigate(NavRoute.BirdDetail.createRoute(birdId))
            }
        )
    }

    composable(
        route = NavRoute.EggProduction.route,
        arguments = listOf(
            navArgument("flockId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) {
        EggProductionScreen(
            onBackClick = { navController.popBackStack() }
        )
    }
}

fun NavGraphBuilder.birdGraph(navController: NavHostController) {
    composable(NavRoute.BirdList.route) {
        BirdListScreen(
            onBackClick = { navController.popBackStack() },
            onAddBirdClick = { navController.navigate(NavRoute.AddBird.createRoute()) },
            onBirdClick = { birdId ->
                navController.navigate(NavRoute.BirdDetail.createRoute(birdId))
            }
        )
    }

    composable(
        route = NavRoute.AddBird.route,
        arguments = listOf(
            navArgument("flockId") {
                type = NavType.LongType
                defaultValue = -1L
            },
            navArgument("species") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val flockIdArg = backStackEntry.arguments?.getLong("flockId") ?: -1L
        val flockId = if (flockIdArg == -1L) null else flockIdArg
        val species = backStackEntry.arguments?.getString("species")

        val breedName by backStackEntry.savedStateHandle.getLiveData<String>(BreedSelectionContract.KEY_BREED_NAME).observeAsState()
        var selectedBreed by remember { mutableStateOf<String?>(null) }

        val ringValue by backStackEntry.savedStateHandle.getLiveData<String>(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_RING_VALUE).observeAsState()

        LaunchedEffect(breedName) {
            if (breedName != null) {
                val result = BreedSelectionContract.consumeSelectionResult(navController)
                if (result != null) {
                    selectedBreed = result.breedName
                }
            }
        }

        AddBirdScreen(
            onBackClick = { navController.popBackStack() },
            onBirdSaved = { _ -> navController.popBackStack() },
            onNavigateToBreedSelection = { speciesId ->
                navController.navigate(NavRoute.BreedSelection.createRoute(speciesId))
            },
            selectedBreed = selectedBreed,
            onClearBreedResult = { selectedBreed = null },
            flockId = flockId,
            lockedSpecies = species
        )
    }

    composable(
        route = NavRoute.BirdDetail.route,
        arguments = listOf(navArgument("birdId") { type = NavType.LongType })
    ) { backStackEntry ->
        val id = backStackEntry.arguments?.getLong("birdId") ?: 0L
        BirdDetailScreen(
            birdId = id,
            onBack = { navController.popBackStack() },
            onRecordSale = { ownerId, ownerType, birdIds ->
                navController.navigate(NavRoute.AddSalesBatch.createRoute(ownerId, ownerType, birdIds))
            }
        )
    }
}

fun NavGraphBuilder.incubationGraph(navController: NavHostController) {
    composable(NavRoute.IncubationList.route) {
        IncubationListScreen(
            onBackClick = { navController.popBackStack() },
            onIncubationClick = { id ->
                navController.navigate(NavRoute.IncubationDetail.createRoute(id))
            },
            onAddIncubationClick = { navController.navigate(NavRoute.AddIncubation.createRoute()) },
            onAddFinancialEntry = { ownerId, ownerType, isRevenue ->
                navController.navigate(NavRoute.AddFinancialEntry.createRoute(ownerId, ownerType, isRevenue))
            },
            onRecordSale = { ownerId, ownerType ->
                navController.navigate(NavRoute.AddSalesBatch.createRoute(ownerId, ownerType))
            },
            onNavigateToEquipment = { navController.navigate(NavRoute.AddDevice.route) }
        )
    }

    composable(
        route = NavRoute.AddIncubation.route,
        arguments = listOf(
            navArgument("flockId") {
                type = NavType.LongType
                defaultValue = -1L
            },
            navArgument("species") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val flockIdArg = backStackEntry.arguments?.getLong("flockId") ?: -1L
        val flockId = if (flockIdArg == -1L) null else flockIdArg
        val species = backStackEntry.arguments?.getString("species")

        val viewModel: com.example.hatchtracker.feature.incubation.AddIncubationViewModel = hiltViewModel()
        val breedId by backStackEntry.savedStateHandle.getLiveData<String>(BreedSelectionContract.KEY_BREED_ID)
            .observeAsState()

        LaunchedEffect(breedId) {
            if (breedId != null) {
                val result = BreedSelectionContract.consumeSelectionResult(navController)
                result?.let { viewModel.setSelectedBreed(it) }
            }
        }

        AddIncubationScreen(
            viewModel = viewModel,
            onNavigateToBreedSelection = { speciesId ->
                navController.navigate(NavRoute.BreedSelection.createRoute(speciesId))
            },
            onBackClick = { navController.popBackStack() },
            onIncubationSaved = { navController.popBackStack() },
            flockId = flockId,
            lockedSpecies = species
        )
    }

    composable(
        route = NavRoute.IncubationDetail.route,
        arguments = listOf(navArgument("incubationId") { type = NavType.LongType })
    ) { backStackEntry ->
        val id = backStackEntry.arguments?.getLong("incubationId") ?: 0L
        val viewModel: com.example.hatchtracker.feature.incubation.IncubationDetailViewModel = hiltViewModel()

        IncubationDetailScreen(
            incubationId = id,
            viewModel = viewModel,
            onBackClick = { navController.popBackStack() },
            onAddFinancialEntry = { ownerId, ownerType, isRevenue ->
                navController.navigate(NavRoute.AddFinancialEntry.createRoute(ownerId, ownerType, isRevenue))
            },
            onRecordSale = { ownerId, ownerType ->
                navController.navigate(NavRoute.AddSalesBatch.createRoute(ownerId, ownerType))
            },
            onDeepdiveClick = { ownerId, ownerType ->
                navController.navigate(NavRoute.FinancialTransactions.createRoute(ownerId, ownerType))
            },
            onRecordHatchClick = { incubationId ->
                navController.navigate(NavRoute.HatchOutcome.createRoute(incubationId))
            },
            onNavigateToTroubleshooting = { incubationId ->
                navController.navigate(NavRoute.Troubleshooting.createRoute(incubationId))
            }
        )
    }

    composable(
        route = NavRoute.Troubleshooting.route,
        arguments = listOf(navArgument("incubationId") { type = NavType.LongType })
    ) { backStackEntry ->
        val incubationId = backStackEntry.arguments?.getLong("incubationId") ?: 0L
        val temp by backStackEntry.savedStateHandle.getLiveData<Double>(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_TEMP_C).observeAsState()
        val humidity by backStackEntry.savedStateHandle.getLiveData<Double>(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_HUMIDITY).observeAsState()

        val viewModel: TroubleshootingViewModel = hiltViewModel()
        TroubleshootingScreen(
            viewModel = viewModel,
            onBackClick = { navController.popBackStack() },
            scannedTemp = temp,
            scannedHumidity = humidity,
            onScanIncubatorClick = { navController.navigate(NavRoute.ScanIncubator.createRoute(incubationId)) },
            onClearIncubatorScan = {
                backStackEntry.savedStateHandle.remove<Double>(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_TEMP_C)
                backStackEntry.savedStateHandle.remove<Double>(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_HUMIDITY)
            }
        )
    }

    composable(NavRoute.HatchPlanner.route) {
        HatchPlannerScreen(
            onBackClick = { navController.popBackStack() }
        )
    }

    composable(
        route = NavRoute.HatchOutcome.route,
        arguments = listOf(navArgument("incubationId") { type = NavType.LongType })
    ) { backStackEntry ->
        val id = backStackEntry.arguments?.getLong("incubationId") ?: 0L
        HatchOutcomeScreen(
            incubationId = id,
            onBackClick = { navController.popBackStack() },
            onHatchRecorded = { navController.popBackStack() }
        )
    }

    composable(
        route = NavRoute.IncubationTimeline.route,
        arguments = listOf(navArgument("incubationId") { type = NavType.LongType })
    ) { backStackEntry ->
        val id = backStackEntry.arguments?.getLong("incubationId") ?: 0L
        val viewModel: com.example.hatchtracker.feature.incubation.IncubationDetailViewModel = hiltViewModel()
        val incubation by viewModel.incubation.collectAsState()
        val dateFormat by viewModel.dateFormat.collectAsState()

        incubation?.let {
            com.example.hatchtracker.feature.incubation.IncubationTimelineScreen(
                incubation = it,
                dateFormat = dateFormat,
                localeFormatService = viewModel.localeFormatService,
                onBackClick = { navController.popBackStack() }
            )
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    composable(
        route = NavRoute.HatchSummary.route,
        arguments = listOf(navArgument("incubationId") { type = NavType.LongType })
    ) { backStackEntry ->
        val id = backStackEntry.arguments?.getLong("incubationId") ?: 0L
        val viewModel: com.example.hatchtracker.feature.incubation.IncubationDetailViewModel = hiltViewModel()
        val incubation by viewModel.incubation.collectAsState()

        incubation?.let { inc ->
            com.example.hatchtracker.feature.incubation.HatchSummaryScreen(
                incubation = inc,
                draftBirds = emptyList(), 
                stats = com.example.hatchtracker.feature.incubation.HatchStats(
                    totalSet = inc.eggsCount,
                    hatched = inc.hatchedCount,
                    infertile = inc.infertileCount,
                    failed = inc.failedCount
                ),
                onBirdChange = { _ -> },
                onConfirm = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

fun NavGraphBuilder.nurseryGraph(navController: NavHostController) {
    composable(NavRoute.Nursery.route) { backStackEntry ->
        val viewModel: com.example.hatchtracker.feature.nursery.NurseryViewModel = hiltViewModel()
        val selectionResult = backStackEntry.savedStateHandle
            .getLiveData<String>(BreedSelectionContract.KEY_BREED_ID)
            .observeAsState()

        LaunchedEffect(selectionResult.value) {
            selectionResult.value?.let { _ ->
                val resultConsumer = BreedSelectionContract.consumeSelectionResult(navController)
                resultConsumer?.let {
                    viewModel.onBreedSelected(it)
                }
            }
        }

        NurseryScreen(
            viewModel = viewModel,
            onBackClick = { navController.popBackStack() },
            onAddFinancialEntry = { ownerId, ownerType, isRevenue ->
                navController.navigate(NavRoute.AddFinancialEntry.createRoute(ownerId, ownerType, isRevenue))
            },
            onSellFlocklet = { ownerId, ownerType ->
                navController.navigate(NavRoute.AddSalesBatch.createRoute(ownerId, ownerType))
            },
            onNavigateToBreedDetail = { breedId ->
                navController.navigate(NavRoute.BreedDetail.createRoute(breedId))
            },
            onNavigateToAddFlock = { species, breeds, flockletId ->
                navController.navigate(NavRoute.AddFlock.createRoute(species, breeds, flockletId))
            },
            onNavigateToBreedSelection = { speciesId ->
                navController.navigate(NavRoute.BreedSelection.createRoute(speciesId))
            },
            onNavigateToIncubation = {
                navController.navigate(NavRoute.IncubationList.route)
            },
            onDeepdiveClick = { ownerId, ownerType ->
                navController.navigate(NavRoute.FinancialTransactions.createRoute(ownerId, ownerType))
            },
            onNavigateToEquipment = { navController.navigate(NavRoute.AddDevice.route) }
        )
    }
}

fun NavGraphBuilder.financialGraph(
    navController: NavHostController,
    profile: UserProfile?,
    subscriptionStateManager: SubscriptionStateManager
) {
    composable(NavRoute.FinancialStats.route) {
        val isAdmin by subscriptionStateManager.isAdmin.collectAsState()
        val isDeveloper by subscriptionStateManager.isDeveloper.collectAsState()
        val hasPrivilege = isAdmin || isDeveloper
        
        val effectiveTier by subscriptionStateManager.effectiveTier.collectAsState()
        val canAccessFinance = FeatureAccessPolicy
            .canAccess(FeatureKey.FINANCE, effectiveTier, hasPrivilege)
            .allowed

        if (!canAccessFinance) {
            LaunchedEffect(Unit) {
                navController.navigate(NavRoute.Paywall.route) {
                    popUpTo(NavRoute.FinancialStats.route) { inclusive = true }
                }
            }
        } else {
            FinancialStatsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToHatchy = { navController.navigate(NavRoute.HatchyChat.route) },
                viewModel = hiltViewModel()
            )
        }
    }

    composable(
        route = NavRoute.AddFinancialEntry.route,
        arguments = listOf(
            navArgument("ownerId") { type = NavType.StringType },
            navArgument("ownerType") { type = NavType.StringType },
            navArgument("isRevenue") { type = NavType.BoolType }
        )
    ) { backStackEntry ->
        val isAdmin by subscriptionStateManager.isAdmin.collectAsState()
        val isDeveloper by subscriptionStateManager.isDeveloper.collectAsState()
        val hasPrivilege = isAdmin || isDeveloper

        val effectiveTier by subscriptionStateManager.effectiveTier.collectAsState()
        val canAccessFinance = FeatureAccessPolicy
            .canAccess(FeatureKey.FINANCE, effectiveTier, hasPrivilege)
            .allowed
        val ownerId = backStackEntry.arguments?.getString("ownerId") ?: ""
        val ownerType = backStackEntry.arguments?.getString("ownerType") ?: ""

        if (!canAccessFinance) {
            LaunchedEffect(Unit) {
                navController.navigate(NavRoute.Paywall.route) {
                    popUpTo(NavRoute.AddFinancialEntry.route) { inclusive = true }
                }
            }
        } else {
            val vendor by backStackEntry.savedStateHandle.getLiveData<String>(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_VENDOR).observeAsState()
            val dateMillis by backStackEntry.savedStateHandle.getLiveData<Long>(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_DATE).observeAsState()
            val total by backStackEntry.savedStateHandle.getLiveData<Double>(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_TOTAL).observeAsState()

            AddFinancialEntryScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() },
                scannedVendor = vendor,
                scannedDateMillis = dateMillis,
                scannedTotal = total,
                onScanReceiptClick = { navController.navigate(NavRoute.ScanReceipt.createRoute(ownerId, ownerType)) },
                onClearReceiptScan = {
                    backStackEntry.savedStateHandle.remove<String>(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_VENDOR)
                    backStackEntry.savedStateHandle.remove<Long>(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_DATE)
                    backStackEntry.savedStateHandle.remove<Double>(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_TOTAL)
                }
            )
        }
    }

    composable(
        route = NavRoute.AddSalesBatch.route,
        arguments = listOf(
            navArgument("ownerId") { type = NavType.StringType },
            navArgument("ownerType") { type = NavType.StringType },
            navArgument("birdIds") { type = NavType.StringType; nullable = true; defaultValue = null }
        )
    ) {
        val isAdmin by subscriptionStateManager.isAdmin.collectAsState()
        val isDeveloper by subscriptionStateManager.isDeveloper.collectAsState()
        val hasPrivilege = isAdmin || isDeveloper

        val effectiveTier by subscriptionStateManager.effectiveTier.collectAsState()
        val canAccessFinance = FeatureAccessPolicy
            .canAccess(FeatureKey.FINANCE, effectiveTier, hasPrivilege)
            .allowed

        if (!canAccessFinance) {
            LaunchedEffect(Unit) {
                navController.navigate(NavRoute.Paywall.route) {
                    popUpTo(NavRoute.AddSalesBatch.route) { inclusive = true }
                }
            }
        } else {
            AddSalesBatchScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }

    composable(
        route = NavRoute.FinancialTransactions.route,
        arguments = listOf(
            navArgument("ownerId") { type = NavType.StringType },
            navArgument("ownerType") { type = NavType.StringType }
        )
    ) {
        val isAdmin by subscriptionStateManager.isAdmin.collectAsState()
        val isDeveloper by subscriptionStateManager.isDeveloper.collectAsState()
        val hasPrivilege = isAdmin || isDeveloper

        val effectiveTier by subscriptionStateManager.effectiveTier.collectAsState()
        val canAccessFinance = FeatureAccessPolicy
            .canAccess(FeatureKey.FINANCE, effectiveTier, hasPrivilege)
            .allowed

        if (!canAccessFinance) {
            LaunchedEffect(Unit) {
                navController.navigate(NavRoute.Paywall.route)
            }
        } else {
            FinancialTransactionsScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }
    }
}

fun NavGraphBuilder.breedingGraph(
    navController: NavHostController,
    profile: UserProfile?,
    subscriptionStateManager: SubscriptionStateManager
) {
    composable(NavRoute.Breeding.route) {
        val isAdmin by subscriptionStateManager.isAdmin.collectAsState()
        val isDeveloper by subscriptionStateManager.isDeveloper.collectAsState()
        val hasPrivilege = isAdmin || isDeveloper

        val effectiveTier by subscriptionStateManager.effectiveTier.collectAsState()
        val canAccessBreeding = FeatureAccessPolicy
            .canAccess(FeatureKey.BREEDING, effectiveTier, hasPrivilege)
            .allowed

        if (!canAccessBreeding) {
             LaunchedEffect(Unit) {
                navController.navigate(NavRoute.Paywall.route)
             }
        } else {
            com.example.hatchtracker.feature.breeding.BreedingHomeScreen(
                onNavigateToInsights = { navController.navigate(NavRoute.BreedSearchInsights.route) },
                onNavigateToPrograms = { navController.navigate(NavRoute.BreedingPrograms.route) },
                onNavigateToWizard = { navController.navigate(NavRoute.BreedingProgramWizard.route) },
                onNavigateToHatchy = { navController.navigate(NavRoute.HatchyChat.route) }
            )
        }
    }

    composable(NavRoute.BreedingPrograms.route) {
        com.example.hatchtracker.feature.breeding.actionplan.BreedingProgramHubScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToDetail = { id -> navController.navigate(NavRoute.BreedingProgramDetail.createRoute(id)) }
        )
    }

    composable(NavRoute.BreedSearchInsights.route) {
        com.example.hatchtracker.feature.breeding.BreedSearchInsightsScreen(
            onBack = { navController.popBackStack() },
            onBreedClick = { breedId -> navController.navigate(NavRoute.BreedDetail.createRoute(breedId)) }
        )
    }

    composable(
        route = NavRoute.BreedingProgramDetail.route,
        arguments = listOf(navArgument("planId") { type = NavType.StringType })
    ) {
        com.example.hatchtracker.feature.breeding.actionplan.BreedingProgramDetailScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToBirdPicker = { _ -> /* TODO */ },
            onNavigateToLinkAssets = { /* TODO */ },
            onNavigateToSelectBirds = { _ -> /* TODO */ }
        )
    }

    // Breeding Program Wizard - Entry is allowed for all, but saving/flock-mode is PRO gated inside.
    composable(NavRoute.BreedingProgramWizard.route) {
        com.example.hatchtracker.feature.breeding.plan.BreedingProgramWizardScreen(
            onBack = { navController.popBackStack() },
            onNavigateToPaywall = { navController.navigate(NavRoute.Paywall.route) }
        )
    }

    composable(
        route = NavRoute.TraitObservation.route,
        arguments = listOf(
            navArgument("breedId") { type = NavType.StringType; nullable = true; defaultValue = null },
            navArgument("parentPairId") { type = NavType.StringType; nullable = true; defaultValue = null },
            navArgument("traitId") { type = NavType.StringType; nullable = true; defaultValue = null }
        )
    ) { backStackEntry ->
        val breedId = backStackEntry.arguments?.getString("breedId") ?: ""
        val parentPairId = backStackEntry.arguments?.getString("parentPairId") ?: ""
        val traitId = backStackEntry.arguments?.getString("traitId") ?: ""
        
        val isAdmin by subscriptionStateManager.isAdmin.collectAsState()
        val isDeveloper by subscriptionStateManager.isDeveloper.collectAsState()
        val hasPrivilege = isAdmin || isDeveloper
        val effectiveTier by subscriptionStateManager.effectiveTier.collectAsState()
        
        val canAccessBreeding = FeatureAccessPolicy
            .canAccess(FeatureKey.BREEDING, effectiveTier, hasPrivilege)
            .allowed

        TraitObservationScreen(
            onBack = { navController.popBackStack() },
            prefilledBreedId = breedId,
            prefilledParentPairId = parentPairId,
            prefilledTraitId = traitId,
            canAccessBreeding = canAccessBreeding,
            onNavigateToPaywall = { navController.navigate(NavRoute.Paywall.route) }
        )
    }

    composable(NavRoute.CommunityValidation.route) {
        val isAdmin by subscriptionStateManager.isAdmin.collectAsState()
        val isDeveloper by subscriptionStateManager.isDeveloper.collectAsState()
        val hasPrivilege = isAdmin || isDeveloper
        val effectiveTier by subscriptionStateManager.effectiveTier.collectAsState()
        
        val canAccessBreeding = FeatureAccessPolicy
            .canAccess(FeatureKey.BREEDING, effectiveTier, hasPrivilege)
            .allowed
        
        com.example.hatchtracker.feature.breeding.CommunityValidationScreen(
            onBack = { navController.popBackStack() },
            canAccessBreeding = canAccessBreeding,
            onNavigateToPaywall = { navController.navigate(NavRoute.Paywall.route) }
        )
    }

    composable(NavRoute.BreedingLocked.route) {
        com.example.hatchtracker.feature.breeding.BreedingLockedScreen(
            onBack = { navController.popBackStack() },
            onViewPlans = { navController.navigate(NavRoute.Paywall.route) }
        )
    }
}

fun NavGraphBuilder.adminGraph(
    navController: NavHostController,
    profile: UserProfile?,
    subscriptionStateManager: SubscriptionStateManager,
    authClaimsRepository: AuthClaimsRepository
) {
    composable(NavRoute.AdminMenu.route) {
        val isAdmin by subscriptionStateManager.isAdmin.collectAsState()
        val isDeveloper by subscriptionStateManager.isDeveloper.collectAsState()
        val hasPrivilege = isAdmin || isDeveloper

        if (hasPrivilege) {
            AdminMenuScreen(
                onSelectBreeds = { navController.navigate(NavRoute.BreedAdmin.route) },
                onSelectTraits = { navController.navigate(NavRoute.TraitPromotion.route) },
                onSelectLogs = { navController.navigate(NavRoute.AdminAuditLog.route) },
                onSelectTickets = { navController.navigate(NavRoute.AdminTicketDashboard.route) },
                
                
                onBack = { navController.popBackStack() }
            )
        } else {
            LaunchedEffect(Unit) {
                navController.navigate(NavRoute.MainMenu.createRoute("home")) {
                    popUpTo(NavRoute.AdminMenu.route) { inclusive = true }
                }
            }
        }
    }
    composable(NavRoute.BreedAdmin.route) {
        val isAdmin by subscriptionStateManager.isAdmin.collectAsState()
        val isDeveloper by subscriptionStateManager.isDeveloper.collectAsState()
        val hasPrivilege = isAdmin || isDeveloper

        if (hasPrivilege) {
            BreedAdminScreen(
                onBack = { navController.popBackStack() }
            )
        } else {
            LaunchedEffect(Unit) { navController.popBackStack() }
        }
    }

    composable(NavRoute.TraitPromotion.route) {
        val isAdmin by subscriptionStateManager.isAdmin.collectAsState()
        val isDeveloper by subscriptionStateManager.isDeveloper.collectAsState()
        val hasPrivilege = isAdmin || isDeveloper
        val effectiveTier by subscriptionStateManager.effectiveTier.collectAsState()

        val canAccessBreeding = FeatureAccessPolicy
            .canAccess(FeatureKey.BREEDING, effectiveTier, hasPrivilege)
            .allowed
        
        // Strict privilege check for the promotion request panel
        if (hasPrivilege) {
            TraitPromotionScreen(
                onBack = { navController.popBackStack() },
                canAccessBreeding = canAccessBreeding,
                onNavigateToPaywall = { navController.navigate(NavRoute.Paywall.route) }
            )
        } else {
            LaunchedEffect(Unit) { navController.popBackStack() }
        }
    }

    composable(NavRoute.AdminAuditLog.route) {
        val isAdmin by subscriptionStateManager.isAdmin.collectAsState()
        val isDeveloper by subscriptionStateManager.isDeveloper.collectAsState()
        val hasPrivilege = isAdmin || isDeveloper

        if (hasPrivilege) {
            AdminAuditLogScreen(
                onBack = { navController.popBackStack() }
            )
        } else {
            LaunchedEffect(Unit) { navController.popBackStack() }
        }
    }

    composable(NavRoute.AdminTicketDashboard.route) {
        val isAdmin by subscriptionStateManager.isAdmin.collectAsState()
        val isDeveloper by subscriptionStateManager.isDeveloper.collectAsState()
        val hasPrivilege = isAdmin || isDeveloper

        if (hasPrivilege) {
            AdminTicketDashboardScreen(
                onBack = { navController.popBackStack() },
                onNavigateToTicket = { ticketId ->
                    navController.navigate(NavRoute.AdminTicketDetail.createRoute(ticketId))
                }
            )
        } else {
            LaunchedEffect(Unit) { navController.popBackStack() }
        }
    }

    composable(
        route = NavRoute.AdminTicketDetail.route,
        arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
    ) { backStackEntry ->
        val isAdmin by subscriptionStateManager.isAdmin.collectAsState()
        val isDeveloper by subscriptionStateManager.isDeveloper.collectAsState()
        val hasPrivilege = isAdmin || isDeveloper

        if (!hasPrivilege) {
            LaunchedEffect(Unit) { navController.popBackStack() }
            return@composable
        }

        val ticketId = backStackEntry.arguments?.getString("ticketId")
        val viewModel: com.example.hatchtracker.feature.admin.AdminSupportViewModel = hiltViewModel()
        val selectedTicket by viewModel.selectedTicket.collectAsState(initial = null)
        val notes by viewModel.internalNotes.collectAsState(initial = emptyList())
        val currentAction by viewModel.currentAction.collectAsState(initial = null)

         LaunchedEffect(ticketId) {
            if (ticketId != null) {
                viewModel.selectTicket(ticketId)
            }
        }

        if (selectedTicket != null) {
            AdminTicketDetailView(
                ticket = selectedTicket!!,
                notes = notes,
                onBack = {
                    viewModel.clearSelection()
                    navController.popBackStack()
                },
                onUpdateStatus = viewModel::updateStatus,
                onAddNote = viewModel::addNote,
                onApprove = viewModel::approveTicket,
                onResolve = viewModel::resolveTicket,
                onReject = viewModel::rejectTicket,
                currentAction = currentAction
            )
        }
    }
}

fun NavGraphBuilder.notificationsGraph(navController: NavHostController) {
    composable(NavRoute.NotificationCenter.route) {
        val viewModel: com.example.hatchtracker.feature.notifications.NotificationCenterViewModel = hiltViewModel()
        com.example.hatchtracker.feature.notifications.NotificationCenterScreen(
            viewModel = viewModel,
            onBackClick = { navController.popBackStack() },
            onIncubationClick = { incubationId ->
                navController.navigate(NavRoute.IncubationDetail.createRoute(incubationId))
            }
        )
    }
}

fun NavGraphBuilder.supportGraph(navController: NavHostController) {
    composable(
        route = NavRoute.UserTicketDetail.route,
        arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
    ) { backStackEntry ->
        val ticketId = backStackEntry.arguments?.getString("ticketId") ?: ""
        val viewModel: com.example.hatchtracker.feature.support.SupportViewModel = hiltViewModel()
        val tickets by viewModel.userTickets.collectAsState()
        val ticket = tickets.find { it.ticketId == ticketId }

        if (ticket != null) {
            com.example.hatchtracker.feature.support.UserTicketDetailPlaceholder(
                ticket = ticket,
                onBack = { navController.popBackStack() }
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

fun NavGraphBuilder.breedSelectionGraph(navController: NavHostController) {
    composable(
        route = NavRoute.BreedSelection.route,
        arguments = listOf(navArgument("speciesId") { type = NavType.StringType })
    ) { backStackEntry ->
        val speciesId = backStackEntry.arguments?.getString("speciesId") ?: ""
        com.example.hatchtracker.feature.breeding.BreedSelectionScreen(
            speciesId = speciesId,
            onBreedSelected = { result ->
                BreedSelectionContract.setSelectionResult(navController, result)
                navController.popBackStack()
            },
            onBackClick = { navController.popBackStack() }
        )
    }

    composable(
        route = NavRoute.BreedDetail.route,
        arguments = listOf(navArgument("breedId") { type = NavType.StringType })
    ) { backStackEntry ->
        val breedId = backStackEntry.arguments?.getString("breedId") ?: ""
        com.example.hatchtracker.feature.breeding.BreedDetailScreen(
            breedId = breedId,
            onBackClick = { navController.popBackStack() }
        )
    }
}

fun NavGraphBuilder.scannerGraph(navController: NavHostController) {
    composable(
        route = NavRoute.ScanRing.route,
        arguments = listOf(navArgument("birdId") { type = NavType.LongType; defaultValue = -1L })
    ) {
        com.example.hatchtracker.core.scanner.ui.ScanRingQrScreen(
            onResult = { result ->
                navController.previousBackStackEntry?.savedStateHandle?.set(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_RING_VALUE, result.rawValue)
                navController.popBackStack()
            },
            onBack = { navController.popBackStack() }
        )
    }

    composable(
        route = NavRoute.ScanIncubator.route,
        arguments = listOf(navArgument("incubationId") { type = NavType.LongType })
    ) {
        com.example.hatchtracker.core.scanner.ui.ScanIncubatorScreen(
            onReadingCaptured = { draft ->
                val handle = navController.previousBackStackEntry?.savedStateHandle
                handle?.set(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_TEMP_C, draft.temperatureC)
                handle?.set(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_HUMIDITY, draft.humidityPercent)
                handle?.set(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_INCUBATOR_WARNINGS, draft.warnings.toTypedArray())
                navController.popBackStack()
            },
            onBack = { navController.popBackStack() }
        )
    }

    composable(
        route = NavRoute.ScanReceipt.route,
        arguments = listOf(
            navArgument("ownerId") { type = NavType.StringType },
            navArgument("ownerType") { type = NavType.StringType }
        )
    ) {
        com.example.hatchtracker.core.scanner.ui.ScanReceiptScreen(
            onReceiptCaptured = { draft ->
                val handle = navController.previousBackStackEntry?.savedStateHandle
                handle?.set(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_VENDOR, draft.vendor)
                handle?.set(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_DATE, draft.dateEpochMillis)
                handle?.set(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_TOTAL, draft.total)
                handle?.set(com.example.hatchtracker.core.navigation.ScannerContracts.KEY_RAW_TEXT, draft.rawText)
                navController.popBackStack()
            },
            onBack = { navController.popBackStack() }
        )
    }
}

fun NavGraphBuilder.moderationGraph(
    navController: NavHostController,
    isCommunityAdmin: Boolean,
    isSystemAdmin: Boolean
) {
    val canAccess = isCommunityAdmin || isSystemAdmin

    composable(NavRoute.ModerationQueue.route) {
        if (canAccess) {
            ModerationQueueScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { reportId ->
                    navController.navigate(NavRoute.ReportDetail.createRoute(reportId))
                }
            )
        } else {
            LaunchedEffect(Unit) {
                navController.popBackStack()
            }
        }
    }

    composable(
        route = NavRoute.ReportDetail.route,
        arguments = listOf(navArgument("reportId") { type = NavType.StringType })
    ) {
        if (canAccess) {
            ReportDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        } else {
            LaunchedEffect(Unit) {
                navController.popBackStack()
            }
        }
    }

    composable(
        route = NavRoute.UserSafety.route,
        arguments = listOf(navArgument("userId") { type = NavType.StringType })
    ) {
        if (canAccess) {
            UserSafetyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        } else {
            LaunchedEffect(Unit) {
                navController.popBackStack()
            }
        }
    }

    composable(
        route = NavRoute.ListingModerationReview.route,
        arguments = listOf(navArgument("listingId") { type = NavType.StringType })
    ) {
        if (canAccess) {
            ListingModerationReviewScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        } else {
            LaunchedEffect(Unit) {
                navController.popBackStack()
            }
        }
    }
}
