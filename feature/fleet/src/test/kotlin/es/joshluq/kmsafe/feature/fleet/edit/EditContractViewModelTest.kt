package es.joshluq.kmsafe.feature.fleet.edit

import androidx.lifecycle.SavedStateHandle
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.GetImageBytesUseCase
import es.joshluq.kmsafe.domain.usecase.GetVehicleByIdUseCase
import es.joshluq.kmsafe.domain.usecase.UpdateContractUseCase
import es.joshluq.kmsafe.domain.usecase.UploadVehicleImageUseCase
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
class EditContractViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getVehicleByIdUseCase: GetVehicleByIdUseCase = mockk()
    private val updateContractUseCase: UpdateContractUseCase = mockk()
    private val uploadVehicleImageUseCase: UploadVehicleImageUseCase = mockk()
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase = mockk()
    private val getImageBytesUseCase: GetImageBytesUseCase = mockk()
    private val analytics: AnalyticskitManager = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)

    private val sampleVehicle = RentingContract(
        id = "v-edit-1",
        vehicleName = "Seat Leon",
        fuelType = es.joshluq.kmsafe.domain.model.FuelType.GASOLINE_95,
        startDate = 1000L,
        durationMonths = 24,
        totalKms = 20000.0,
        startOdometer = 5000.0,
        currentOdometer = 8000.0,
        excessDistancePrice = 0.10,
        courtesyMarginKms = 100.0
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.CLOUD_SYNC)) } returns flowOf(
            CheckFeatureAccessUseCase.Output.Success(isGranted = true)
        )
        every { getVehicleByIdUseCase(GetVehicleByIdUseCase.Input("v-edit-1")) } returns flowOf(
            GetVehicleByIdUseCase.Output.Success(sampleVehicle)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun createViewModel(vehicleId: String = "v-edit-1"): EditContractViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("vehicleId" to vehicleId))
        return EditContractViewModel(
            savedStateHandle = savedStateHandle,
            getVehicleByIdUseCase = getVehicleByIdUseCase,
            updateContractUseCase = updateContractUseCase,
            uploadVehicleImageUseCase = uploadVehicleImageUseCase,
            checkFeatureAccessUseCase = checkFeatureAccessUseCase,
            getImageBytesUseCase = getImageBytesUseCase,
            analytics = analytics,
            logger = logger
        )
    }

    @Test
    fun `given vehicle loaded when initialized then state contains vehicle info and is not dirty`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Seat Leon", viewModel.state.value.vehicleName)
        assertEquals(es.joshluq.kmsafe.domain.model.FuelType.GASOLINE_95, viewModel.state.value.fuelType)
        assertFalse(viewModel.state.value.isDirty)
    }

    @Test
    fun `given field edited then isDirty becomes true`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isDirty)

        viewModel.sendEvent(Event.OnVehicleNameChanged("Seat Leon Cupra"))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isDirty)
    }

    @Test
    fun `given save clicked with valid changes when update succeeds then emits NavigateBack`() = runTest(testDispatcher) {
        every { updateContractUseCase(any()) } returns flowOf(
            UpdateContractUseCase.Output.Success
        )

        val effects = mutableListOf<Effect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnVehicleNameChanged("Seat Leon Updated"))
        viewModel.sendEvent(Event.OnSaveClicked)
        advanceUntilIdle()

        coVerify { updateContractUseCase(any()) }
        assertEquals(1, effects.size)
        assertEquals(Effect.NavigateBack, effects.first())
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
}
