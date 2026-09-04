package es.joshluq.kmsafe.feature.fleet.setup

import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.GetImageBytesUseCase
import es.joshluq.kmsafe.domain.usecase.SaveInitialContractUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupWizardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val saveInitialContractUseCase: SaveInitialContractUseCase = mockk()
    private val getEntitlementsUseCase: GetEntitlementsUseCase = mockk()
    private val uploadVehicleImageUseCase: UploadVehicleImageUseCase = mockk()
    private val getImageBytesUseCase: GetImageBytesUseCase = mockk()
    private val analytics: AnalyticskitManager = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getEntitlementsUseCase(any()) } returns flowOf(
            GetEntitlementsUseCase.Output.Success(Entitlements.Default.copy(subscriptionLevel = SubscriptionLevel.PREMIUM))
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun createViewModel(): SetupWizardViewModel {
        return SetupWizardViewModel(
            saveInitialContractUseCase = saveInitialContractUseCase,
            getEntitlementsUseCase = getEntitlementsUseCase,
            uploadVehicleImage = uploadVehicleImageUseCase,
            getImageBytesUseCase = getImageBytesUseCase,
            analytics = analytics,
            logger = logger
        )
    }

    @Test
    fun `given initial state when initialized then step is VEHICLE_IDENTITY and isPremium is set`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(SetupStep.VEHICLE_IDENTITY, viewModel.state.value.currentStep)
        assertEquals(SubscriptionLevel.PREMIUM, viewModel.state.value.subscriptionLevel)
    }

    @Test
    fun `given blank vehicle name when next clicked then shows validation error and does not advance`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnVehicleNameChanged(""))
        viewModel.sendEvent(Event.OnNextClicked)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.vehicleNameError)
        assertEquals(SetupStep.VEHICLE_IDENTITY, viewModel.state.value.currentStep)
    }

    @Test
    fun `given valid vehicle name when next clicked then advances to CONTRACT_TIMEFRAME`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnVehicleNameChanged("Tesla Model 3"))
        viewModel.sendEvent(Event.OnNextClicked)
        advanceUntilIdle()

        assertNull(viewModel.state.value.vehicleNameError)
        assertEquals(SetupStep.CONTRACT_TIMEFRAME, viewModel.state.value.currentStep)
    }

    @Test
    fun `given step navigation through wizard when all steps valid then saves contract and navigates to dashboard`() = runTest(testDispatcher) {
        every { saveInitialContractUseCase(any()) } returns flowOf(
            SaveInitialContractUseCase.Output.Success("contract-new-id")
        )

        val effects = mutableListOf<Effect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        // Step 1: Vehicle Identity
        viewModel.sendEvent(Event.OnVehicleNameChanged("Tesla Model 3"))
        viewModel.sendEvent(Event.OnFuelTypeChanged(es.joshluq.kmsafe.domain.model.FuelType.ELECTRIC_KWH))
        viewModel.sendEvent(Event.OnNextClicked)
        advanceUntilIdle()
        assertEquals(SetupStep.CONTRACT_TIMEFRAME, viewModel.state.value.currentStep)

        // Step 2: Contract Timeframe
        viewModel.sendEvent(Event.OnStartDateChanged("2024-01-01"))
        viewModel.sendEvent(Event.OnDurationMonthsChanged("36"))
        viewModel.sendEvent(Event.OnNextClicked)
        advanceUntilIdle()
        assertEquals(SetupStep.MILEAGE_BUDGET, viewModel.state.value.currentStep)

        // Step 3: Mileage Budget
        viewModel.sendEvent(Event.OnTotalKmsChanged("45000"))
        viewModel.sendEvent(Event.OnStartOdometerChanged("10000"))
        viewModel.sendEvent(Event.OnCurrentOdometerChanged("12000"))
        viewModel.sendEvent(Event.OnNextClicked)
        advanceUntilIdle()
        assertEquals(SetupStep.SMART_ACTIVATION, viewModel.state.value.currentStep)

        // Step 4: Smart Activation
        viewModel.sendEvent(Event.OnBluetoothDeviceSelected("Car BT", "00:11:22:33:44:55"))
        viewModel.sendEvent(Event.OnNextClicked)
        advanceUntilIdle()
        assertEquals(SetupStep.ADVANCED_PROTECTION, viewModel.state.value.currentStep)

        // Step 5: Advanced Protection
        viewModel.sendEvent(Event.OnExcessDistancePriceChanged("0.15"))
        viewModel.sendEvent(Event.OnCourtesyMarginKmsChanged("200"))
        viewModel.sendEvent(Event.OnNextClicked)
        advanceUntilIdle()

        coVerify { saveInitialContractUseCase(any()) }
        assertEquals(1, effects.size)
        assertEquals(Effect.NavigateToDashboard, effects.first())
    }

    @Test
    fun `given back clicked on second step then returns to previous step`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnVehicleNameChanged("Tesla Model 3"))
        viewModel.sendEvent(Event.OnNextClicked)
        advanceUntilIdle()
        assertEquals(SetupStep.CONTRACT_TIMEFRAME, viewModel.state.value.currentStep)

        viewModel.sendEvent(Event.OnBackClicked)
        advanceUntilIdle()
        assertEquals(SetupStep.VEHICLE_IDENTITY, viewModel.state.value.currentStep)
    }

    @Test
    fun `given back clicked on first step then emits NavigateBack effect`() = runTest(testDispatcher) {
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
