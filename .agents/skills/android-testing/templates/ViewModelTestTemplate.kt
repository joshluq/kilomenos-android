package com.example.feature.ui

import app.cash.turbine.test
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureViewModelTest {

    class MainDispatcherRule(
        val testDispatcher: TestDispatcher = StandardTestDispatcher()
    ) : TestWatcher() {
        override fun starting(description: Description) {
            Dispatchers.setMain(testDispatcher)
        }
        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleUseCase: SampleUseCase = mockk()
    private lateinit var viewModel: FeatureViewModel

    @Before
    fun setUp() {
        clearAllMocks()
        viewModel = FeatureViewModel(
            sampleUseCase = sampleUseCase,
            dispatcher = mainDispatcherRule.testDispatcher
        )
    }

    @Test
    fun ac01_whenInitialLoad_emitsLoadingThenSuccess() = runTest {
        coEvery { sampleUseCase.execute() } returns Result.success(listOf("item1"))

        viewModel.uiState.test {
            assertEquals(FeatureUiState.Loading, awaitItem())
            viewModel.handleAction(FeatureUiAction.LoadData)
            val success = awaitItem() as FeatureUiState.Success
            assertEquals(1, success.items.size)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { sampleUseCase.execute() }
    }
}
