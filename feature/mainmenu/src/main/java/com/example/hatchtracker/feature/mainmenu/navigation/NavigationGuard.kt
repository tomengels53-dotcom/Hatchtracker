package com.example.hatchtracker.feature.mainmenu.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.hatchtracker.auth.SessionState
import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import com.example.hatchtracker.core.navigation.NavRoute
import com.example.hatchtracker.data.models.SubscriptionTier
import com.example.hatchtracker.domain.model.UserProfile

/**
 * A centralized Guard that watches navigation events and enforces session security.
 * It prevents unauthorized access to protected routes by redirecting to Login.
 */
@Composable
fun NavigationGuard(
    navController: NavController,
    sessionState: SessionState,
    profile: UserProfile?,
    subscriptionStateManager: com.example.hatchtracker.billing.SubscriptionStateManager
) {
    val currentEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = currentEntry?.destination?.route

    val isAdmin by subscriptionStateManager.isAdmin.collectAsState()
    val isDeveloper by subscriptionStateManager.isDeveloper.collectAsState()
    val effectiveTier by subscriptionStateManager.effectiveTier.collectAsState()

    LaunchedEffect(currentRoute, sessionState, isAdmin, isDeveloper, effectiveTier) {
        val isPublicRoute = currentRoute == NavRoute.Welcome.route ||
            currentRoute == NavRoute.Login.route ||
            currentRoute == NavRoute.SignUp.route ||
            currentRoute == null // Initial state might be null
            
        val isAdminRoute = currentRoute in setOf(
            NavRoute.AdminMenu.route,
            NavRoute.AdminAuditLog.route,
            NavRoute.AdminTicketDashboard.route,
            NavRoute.AdminTicketDetail.route,
            NavRoute.BreedAdmin.route,
            NavRoute.TraitPromotion.route
        )

        if (!isPublicRoute) {
            when (sessionState) {
                is SessionState.Authenticated -> {
                    val hasPrivilege = isAdmin || isDeveloper
                    val canAccessBreeding = FeatureAccessPolicy
                        .canAccess(FeatureKey.BREEDING, effectiveTier, hasPrivilege)
                        .allowed
                    val canAccessFinance = FeatureAccessPolicy
                        .canAccess(FeatureKey.FINANCE, effectiveTier, hasPrivilege)
                        .allowed

                    if (isAdminRoute && !hasPrivilege) {
                        navController.navigate(NavRoute.MainMenu.createRoute("home")) {
                            popUpTo(currentRoute) { inclusive = true }
                            launchSingleTop = true
                        }
                    } 
                    /* 
                    Hard redirects removed in favor of in-UI upsell dialogs.
                    else if (isBreedingRoute && !canAccessBreeding) {
                        navController.navigate(NavRoute.Paywall.route) {
                            popUpTo(currentRoute) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else if (isFinanceRoute && !canAccessFinance) {
                        navController.navigate(NavRoute.Paywall.route) {
                            popUpTo(currentRoute) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    */
                }
                is SessionState.Initializing -> {
                    // Loading. Ideally show loading, but for guard purposes, we wait.
                }
                else -> {
                    navController.navigate(NavRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}
