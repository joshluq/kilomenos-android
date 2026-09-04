package es.joshluq.kmsafe.feature.auth.launch

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.service.FingerprintProvider
import es.joshluq.kmsafe.domain.usecase.CheckSessionUseCase
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.SignOutUseCase
import es.joshluq.kmsafe.domain.usecase.SyncContractsUseCase
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LaunchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val checkSessionUseCase: CheckSessionUseCase = mockk(relaxed = true)
    private val syncContractsUseCase: SyncContractsUseCase = mockk(relaxed = true)
    private val getEntitlementsUseCase: GetEntitlementsUseCase = mockk(relaxed = true)
    private val signOutUseCase: SignOutUseCase = mockk(relaxed = true)
    private val fingerprintProvider: FingerprintProvider = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { fingerprintProvider.getFingerprint() } returns "test_fingerprint"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun createViewModel(): LaunchViewModel {
        return LaunchViewModel(
            checkSessionUseCase = checkSessionUseCase,
            syncContractsUseCase = syncContractsUseCase,
            getEntitlementsUseCase = getEntitlementsUseCase,
            signOutUseCase = signOutUseCase,
            fingerprintProvider = fingerprintProvider,
            logger = logger
        )
    }

    @Test
    fun `given idle session when initialized then emits NavigateToLogin effect`() = runTest(testDispatcher) {
        every { checkSessionUseCase(CheckSessionUseCase.Input) } returns flowOf(
            CheckSessionUseCase.Output.Progress,
            CheckSessionUseCase.Output.IdleSession
        )

        val effects = mutableListOf<LaunchEffect>()
        val viewModel = createViewModel()

        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(LaunchEffect.NavigateToLogin, effects.first())
    }

    @Test
    fun `given active session when initialized then syncs entitlements and contracts and navigates to dashboard`() = runTest(testDispatcher) {
        every { checkSessionUseCase(CheckSessionUseCase.Input) } returns flowOf(
            CheckSessionUseCase.Output.ActiveSession
        )
        val entitlements = Entitlements.Default.copy(subscriptionLevel = SubscriptionLevel.PREMIUM)
        every { getEntitlementsUseCase(any()) } returns flowOf(
            GetEntitlementsUseCase.Output.Success(entitlements)
        )
        every { syncContractsUseCase(any()) } returns flowOf(
            SyncContractsUseCase.Output.Success
        )

        val effects = mutableListOf<LaunchEffect>()
        val viewModel = createViewModel()

        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        advanceUntilIdle()

        coVerify { getEntitlementsUseCase(GetEntitlementsUseCase.Input("test_fingerprint", forceRefresh = true)) }
        coVerify { syncContractsUseCase(SyncContractsUseCase.Input) }
        assertEquals(1, effects.size)
        assertEquals(LaunchEffect.NavigateToDashboard, effects.first())
    }

    @Test
    fun `given inconsistent session when initialized then forces sign out and navigates to login`() = runTest(testDispatcher) {
        every { checkSessionUseCase(CheckSessionUseCase.Input) } returns flowOf(
            CheckSessionUseCase.Output.InconsistentSession
        )
        every { signOutUseCase(SignOutUseCase.Input(clearLocalData = true)) } returns flowOf(
            SignOutUseCase.Output.Success
        )

        val effects = mutableListOf<LaunchEffect>()
        val viewModel = createViewModel()

        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        advanceUntilIdle()

        coVerify { signOutUseCase(SignOutUseCase.Input(clearLocalData = true)) }
        assertEquals(1, effects.size)
        assertEquals(LaunchEffect.NavigateToLogin, effects.first())
    }

    @Test
    fun `given active session with failing syncs when initialized then navigates to dashboard regardless`() = runTest(testDispatcher) {
        every { checkSessionUseCase(CheckSessionUseCase.Input) } returns flowOf(
            CheckSessionUseCase.Output.ActiveSession
        )
        every { getEntitlementsUseCase(any()) } returns flowOf(
            GetEntitlementsUseCase.Output.Failure("Network error")
        )
        every { syncContractsUseCase(any()) } returns flowOf(
            SyncContractsUseCase.Output.Failure("Network error")
        )

        val effects = mutableListOf<LaunchEffect>()
        val viewModel = createViewModel()

        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(LaunchEffect.NavigateToDashboard, effects.first())
    }
}
