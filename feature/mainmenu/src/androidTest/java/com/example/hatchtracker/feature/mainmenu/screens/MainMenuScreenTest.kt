package com.example.hatchtracker.feature.mainmenu.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class MainMenuScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testMainMenuPrePolishLayoutStructure() {
        rule.setContent {
            HomeMenuContent(
                caps = com.example.hatchtracker.data.models.SubscriptionCapabilities(
                    tier = com.example.hatchtracker.data.models.SubscriptionTier.FREE,
                    maxFlocks = 1,
                    maxIncubators = 1,
                    canExportData = false,
                    canAccessAdvancedAnalytics = false
                ),
                canAccessAdmin = false,
                isAdmin = false,
                hasPremiumAccess = false,
                onNavigateToFlock = {},
                onNavigateToIncubation = {},
                onNavigateToBreeding = {},
                onNavigateToNursery = {},
                onNavigateToFinance = {},
                onSettingsClick = {},
                onNavigateToPaywall = {},
                onNavigateToAdmin = {},
                onShowLockedFeature = {}
            )
        }

        // 1. Verify HERO Card exists (contains Hatchy branding or specific tagline)
        rule.onNodeWithText("HatchBase", substring = true, ignoreCase = true)
            .assertExists()
            .assertIsDisplayed()

        // 2. Verify iconic ModuleTile grid exists (Should be exactly 4 basic modules)
        rule.onNodeWithText("Breeding", ignoreCase = true).assertExists()
        rule.onNodeWithText("Flocks", ignoreCase = true).assertExists()
        rule.onNodeWithText("Incubators", ignoreCase = true).assertExists()
        rule.onNodeWithText("Nursery", ignoreCase = true).assertExists()

        // 3. Verify PremiumModuleTile exists
        rule.onNodeWithText("Unlock Premium", substring = true, ignoreCase = true).assertExists()
    }
}
