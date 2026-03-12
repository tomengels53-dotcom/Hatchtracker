package com.example.hatchtracker.feature.flock

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class AddFlockScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testAddFlockScreenPrePolishStructure() {
        // Set up empty standard screen state
        rule.setContent {
            com.example.hatchtracker.feature.flock.ui.screens.AddFlockScreen(
                onBackClick = {},
                onFlockSaved = {},
                prefilledSpecies = "Chicken",
                prefilledBreeds = emptyList(),
                graduatingFlockletId = null
            )
        }

        // 1. Verify Scaffold/TopBar structure holds
        rule.onNodeWithText("Add New Flock", ignoreCase = true)
            .assertExists()
            .assertIsDisplayed()

        // 2. Verify basic fields
        rule.onNodeWithTag("FlockNameInput").assertExists()
        
        // 3. Verify specifically the structural inline 'Add Breed' button wrapper
        rule.onNodeWithTag("BreedSelector").assertExists()
        rule.onNodeWithText("Add Breed", ignoreCase = true).assertExists()
        
        // 4. Verify save action structure
        rule.onNodeWithTag("SaveFlockButton").assertExists()
    }
}
