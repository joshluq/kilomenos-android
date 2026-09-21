package com.example.profile

import com.example.profile.data.ProfileRepository
import com.example.profile.model.UserProfile
import com.example.profile.ui.ProfileViewModel
import com.example.profile.ui.ProfileUiAction
import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ProfileViewModelTest {
    private val repository: ProfileRepository = mockk(relaxed = true)

    @Test
    fun testInitialLoad() = runTest {
        every { repository.getProfile() } returns flowOf(UserProfile("1", "Alice", false))
        val viewModel = ProfileViewModel(repository)
        viewModel.uiState.test {
            val item = awaitItem()
            assertEquals("Alice", item.profile?.name)
        }
    }

    @Test
    fun testOfflineToggle() = runTest {
        val viewModel = ProfileViewModel(repository)
        viewModel.onAction(ProfileUiAction.ToggleTheme)
        coVerify { repository.updateTheme(any()) }
    }
}
