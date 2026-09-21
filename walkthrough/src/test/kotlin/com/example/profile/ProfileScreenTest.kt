package com.example.profile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.profile.model.UserProfile
import com.example.profile.ui.ProfileScreen
import com.example.profile.ui.ProfileUiState
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testProfileDisplaysName() {
        val state = ProfileUiState(profile = UserProfile("1", "Bob"))
        composeTestRule.setContent {
            ProfileScreen(state = state, onAction = {})
        }
        composeTestRule.onNodeWithText("Bob").assertExists()
    }
}
