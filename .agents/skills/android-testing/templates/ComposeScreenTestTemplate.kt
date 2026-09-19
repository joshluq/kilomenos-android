package com.example.feature.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeatureScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ac01_initialLoading_showsProgressIndicator() {
        composeTestRule.setContent {
            FeatureScreen(
                state = FeatureUiState.Loading,
                onAction = {}
            )
        }

        composeTestRule.onNodeWithTag("loading_spinner")
            .assertIsDisplayed()
    }

    @Test
    fun ac02_whenActionClicked_dispatchesUiAction() {
        var actionDispatched = false
        composeTestRule.setContent {
            FeatureScreen(
                state = FeatureUiState.Success(items = listOf("item1")),
                onAction = { actionDispatched = true }
            )
        }

        composeTestRule.onNodeWithTag("submit_button")
            .assertIsEnabled()
            .performClick()

        assertTrue(actionDispatched)
    }
}
