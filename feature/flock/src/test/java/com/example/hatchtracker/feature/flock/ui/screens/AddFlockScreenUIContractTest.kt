package com.example.hatchtracker.feature.flock.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.hatchtracker.feature.flock.ui.viewmodels.FlockViewModel
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AddFlockScreenUIContractTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun addFlockScreen_displaysCorrectly() {
        val mockViewModel = mockk<FlockViewModel>(relaxed = true)

        composeTestRule.setContent {
            AddFlockScreen(
                onBackClick = {},
                onFlockSaved = {},
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithTag("AddFlockScreen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("FlockNameInput").assertIsDisplayed()
        composeTestRule.onNodeWithTag("SpeciesSelector").assertIsDisplayed()
        // Breed selector might be conditionally displayed or just a box. It's tagged on the Box.
        // It requires species to be selected to show "Add Breed" button or chips?
        // Let's check the code. "if (species.isNotBlank())".
        // Prefilled species is null by default.
        // So BreedSelector might NOT be displayed initially.
        // I won't assert exact visibility of BreedSelector yet unless I specific state.
    }
}
