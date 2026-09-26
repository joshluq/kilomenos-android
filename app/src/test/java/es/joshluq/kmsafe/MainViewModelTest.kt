package es.joshluq.kmsafe

import android.content.Intent
import android.net.Uri
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.core.navigation.Destination
import es.joshluq.kmsafe.core.navigation.NavigationResultStore
import es.joshluq.kmsafe.domain.usecase.CheckSessionUseCase
import es.joshluq.kmsafe.domain.usecase.RegisterDeviceTokenUseCase
import es.joshluq.kmsafe.infrastructure.worker.SyncManager
import es.joshluq.kmsafe.ui.util.NetworkConnectivityObserver
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val checkSessionUseCase: CheckSessionUseCase = mockk(relaxed = true)
    private val registerDeviceTokenUseCase: RegisterDeviceTokenUseCase = mockk(relaxed = true)
    private val connectivityObserver: NetworkConnectivityObserver = mockk(relaxed = true)
    private val syncManager: SyncManager = mockk(relaxed = true)
    private val resultStore: NavigationResultStore = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)

    private val connectivityFlow = MutableSharedFlow<NetworkConnectivityObserver.Status>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { connectivityObserver.observe() } returns connectivityFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `given active session emitted when ViewModel initialized then triggers token registration`() = runTest(testDispatcher) {
        every { checkSessionUseCase(CheckSessionUseCase.Input) } returns flowOf(
            CheckSessionUseCase.Output.Progress,
            CheckSessionUseCase.Output.ActiveSession
        )
        coEvery { registerDeviceTokenUseCase(any()) } returns Result.success(RegisterDeviceTokenUseCase.Output.Success)

        createViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            registerDeviceTokenUseCase(RegisterDeviceTokenUseCase.Input())
        }
    }

    @Test
    fun `given idle session emitted when ViewModel initialized then does not trigger token registration`() = runTest(testDispatcher) {
        every { checkSessionUseCase(CheckSessionUseCase.Input) } returns flowOf(
            CheckSessionUseCase.Output.IdleSession
        )

        createViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) {
            registerDeviceTokenUseCase(any())
        }
    }

    @Test
    fun `given network becomes available when observed then triggers sync after delay`() = runTest(testDispatcher) {
        createViewModel()
        testDispatcher.scheduler.runCurrent()

        connectivityFlow.emit(NetworkConnectivityObserver.Status.Available)
        testDispatcher.scheduler.runCurrent()

        testDispatcher.scheduler.advanceTimeBy(500)
        verify(exactly = 0) { syncManager.scheduleSync() }

        testDispatcher.scheduler.advanceTimeBy(600)
        verify(exactly = 1) { syncManager.scheduleSync() }
    }

    @Test
    fun `given network is unavailable when observed then does not trigger sync`() = runTest(testDispatcher) {
        createViewModel()
        testDispatcher.scheduler.runCurrent()

        connectivityFlow.emit(NetworkConnectivityObserver.Status.Unavailable)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 0) { syncManager.scheduleSync() }
    }

    @Test
    fun `given null intent when handleIntent called then does nothing`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.handleIntent(null)

        verify(exactly = 0) { resultStore.setResult(any(), any()) }
    }

    @Test
    fun `given intent with quick_add action when handleIntent called then sets result in store`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val mockIntent = mockk<Intent>()
        val mockUri = mockk<Uri>()

        every { mockIntent.data } returns mockUri
        every { mockUri.scheme } returns "https"
        every { mockUri.host } returns "kmsafe.app"
        every { mockUri.pathSegments } returns listOf("dashboard")
        every { mockUri.getQueryParameter("action") } returns "quick_add"

        viewModel.handleIntent(mockIntent)

        verify(exactly = 1) { resultStore.setResult("deep_link_destination", Destination.Dashboard) }
        verify(exactly = 1) { resultStore.setResult("quick_add_odometer", true) }
    }

    private fun createViewModel() = MainViewModel(
        checkSessionUseCase = checkSessionUseCase,
        registerDeviceTokenUseCase = registerDeviceTokenUseCase,
        connectivityObserver = connectivityObserver,
        syncManager = syncManager,
        resultStore = resultStore,
        logger = logger
    )
}

