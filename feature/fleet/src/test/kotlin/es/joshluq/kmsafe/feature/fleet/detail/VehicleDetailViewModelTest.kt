package es.joshluq.kmsafe.feature.fleet.detail

import androidx.lifecycle.SavedStateHandle
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteContractUseCase
import es.joshluq.kmsafe.domain.usecase.GetVehicleByIdUseCase
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getVehicleByIdUseCase: GetVehicleByIdUseCase = mockk()
    private val deleteContractUseCase: DeleteContractUseCase = mockk()
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase = mockk()
    private val analytics: AnalyticskitManager = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)

    private val sampleVehicle = RentingContract(
        id = "vehicle-123",
        vehicleName = "BMW Serie 1",
        startDate = 1000L,
        durationMonths = 36,
        totalKms = 30000.0,
        startOdometer = 10000.0,
        currentOdometer = 15000.0,
        isSelected = false
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.CLOUD_SYNC)) } returns flowOf(
            CheckFeatureAccessUseCase.Output.Success(isGranted = true)
        )
        every { getVehicleByIdUseCase(GetVehicleByIdUseCase.Input("vehicle-123")) } returns flowOf(
            GetVehicleByIdUseCase.Output.Success(sampleVehicle)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun createViewModel(vehicleId: String = "vehicle-123"): VehicleDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("vehicleId" to vehicleId))
        return VehicleDetailViewModel(
            savedStateHandle = savedStateHandle,
            getVehicleByIdUseCase = getVehicleByIdUseCase,
            deleteContractUseCase = deleteContractUseCase,
            checkFeatureAccessUseCase = checkFeatureAccessUseCase,
            analytics = analytics,
            logger = logger
        )
    }

    @Test
    fun `given vehicle loaded when initialized then state contains vehicle details`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.isPremium)
        assertEquals(sampleVehicle, viewModel.state.value.renting)
    }

    @Test
    fun `given back clicked event then emits NavigateBack effect`() = runTest(testDispatcher) {
        val effects = mutableListOf<Effect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnBackClicked)
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(Effect.NavigateBack, effects.first())
    }

    @Test
    fun `given edit clicked event then emits NavigateToEdit effect`() = runTest(testDispatcher) {
        val effects = mutableListOf<Effect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnEditClicked)
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(Effect.NavigateToEdit("vehicle-123"), effects.first())
    }

    @Test
    fun `given delete selected vehicle then sets error in state`() = runTest(testDispatcher) {
        val selectedVehicle = sampleVehicle.copy(isSelected = true)
        every { getVehicleByIdUseCase(GetVehicleByIdUseCase.Input("vehicle-123")) } returns flowOf(
            GetVehicleByIdUseCase.Output.Success(selectedVehicle)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnDeleteClicked)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
    }

    @Test
    fun `given confirm delete when success then deletes vehicle and navigates back`() = runTest(testDispatcher) {
        every { deleteContractUseCase(DeleteContractUseCase.Input("vehicle-123")) } returns flowOf(
            DeleteContractUseCase.Output.Success
        )

        val effects = mutableListOf<Effect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnConfirmDelete)
        advanceUntilIdle()

        coVerify { deleteContractUseCase(DeleteContractUseCase.Input("vehicle-123")) }
        assertEquals(1, effects.size)
        assertEquals(Effect.NavigateBack, effects.first())
    }
}
