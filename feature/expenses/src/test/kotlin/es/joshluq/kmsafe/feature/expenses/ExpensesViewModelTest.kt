package es.joshluq.kmsafe.feature.expenses

import androidx.lifecycle.SavedStateHandle
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteFuelExpenseUseCase
import es.joshluq.kmsafe.domain.usecase.GetAllServiceStationsUseCase
import es.joshluq.kmsafe.domain.usecase.GetElectrificationSavingsUseCase
import es.joshluq.kmsafe.domain.usecase.GetExpensesByVehicleUseCase
import es.joshluq.kmsafe.domain.usecase.GetStationVolatilityUseCase
import es.joshluq.kmsafe.domain.usecase.SaveFuelExpenseUseCase
import es.joshluq.kmsafe.domain.usecase.SaveServiceStationUseCase
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
class ExpensesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getExpensesByVehicleUseCase: GetExpensesByVehicleUseCase = mockk()
    private val saveFuelExpenseUseCase: SaveFuelExpenseUseCase = mockk()
    private val deleteFuelExpenseUseCase: DeleteFuelExpenseUseCase = mockk()
    private val getStationVolatilityUseCase: GetStationVolatilityUseCase = mockk()
    private val getElectrificationSavingsUseCase: GetElectrificationSavingsUseCase = mockk()
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase = mockk()
    private val getAllServiceStationsUseCase: GetAllServiceStationsUseCase = mockk()
    private val saveServiceStationUseCase: SaveServiceStationUseCase = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private val sampleExpense = FuelExpense(
        id = "exp-1",
        vehicleId = "v1",
        stationId = "st-1",
        stationName = "Repsol Center",
        timestamp = 1000L,
        fuelType = FuelType.GASOLINE_95,
        unitPrice = 1.65,
        volumeQuantity = 40.0,
        totalCost = 66.0,
        odometerAtExpense = 15000.0,
        isFullTank = true
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.ADVANCED_PROJECTIONS)) } returns flowOf(
            CheckFeatureAccessUseCase.Output.Success(isGranted = true)
        )
        every { getExpensesByVehicleUseCase(any()) } returns flowOf(
            GetExpensesByVehicleUseCase.Output.Success(
                vehicleId = "v1",
                vehicleName = "Audi A3",
                expenses = listOf(sampleExpense),
                currentMonthTotalCost = 66.0,
                currentMonthTotalVolume = 40.0,
                allTimeTotalCost = 66.0,
                currentOdometer = 15000.0,
                lastRefuelTimestamp = 1000L,
                kmSinceLastFullRefuel = null,
                recordsSinceLastRefuel = emptyList(),
                defaultFuelType = FuelType.GASOLINE_95
            )
        )
        every { getElectrificationSavingsUseCase(any()) } returns flowOf(
            GetElectrificationSavingsUseCase.Output.Success(
                totalKwh = 0.0,
                totalElectricCost = 0.0,
                theoreticalGasolineCost = 0.0,
                totalSavedEuros = 0.0,
                equivalentKmDriven = 0.0
            )
        )
        every { getAllServiceStationsUseCase(any()) } returns flowOf(
            GetAllServiceStationsUseCase.Output.Empty
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun createViewModel(handle: SavedStateHandle = SavedStateHandle()): ExpensesViewModel {
        return ExpensesViewModel(
            savedStateHandle = handle,
            getExpensesByVehicleUseCase = getExpensesByVehicleUseCase,
            saveFuelExpenseUseCase = saveFuelExpenseUseCase,
            deleteFuelExpenseUseCase = deleteFuelExpenseUseCase,
            getStationVolatilityUseCase = getStationVolatilityUseCase,
            getElectrificationSavingsUseCase = getElectrificationSavingsUseCase,
            checkFeatureAccessUseCase = checkFeatureAccessUseCase,
            getAllServiceStationsUseCase = getAllServiceStationsUseCase,
            saveServiceStationUseCase = saveServiceStationUseCase,
            logger = logger
        )
    }

    @Test
    fun `given expenses loaded when initialized then state contains expenses and vehicle info`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.isPremium)
        assertEquals("v1", viewModel.state.value.vehicleId)
        assertEquals("Audi A3", viewModel.state.value.vehicleName)
        assertEquals(1, viewModel.state.value.expenses.size)
        assertEquals(66.0, viewModel.state.value.currentMonthTotalCost, 0.01)
    }

    @Test
    fun `given filter changed to ELECTRIC when no electric expenses then filtered list is empty`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(ExpensesEvent.OnFilterChanged(ExpenseFilterMode.ELECTRIC))
        advanceUntilIdle()

        assertEquals(ExpenseFilterMode.ELECTRIC, viewModel.state.value.filterMode)
        assertTrue(viewModel.state.value.filteredExpenses.isEmpty())
    }

    @Test
    fun `given open add expense event then sets isAddExpenseSheetOpen to true`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isAddExpenseSheetOpen)

        viewModel.sendEvent(ExpensesEvent.OnOpenAddExpense)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isAddExpenseSheetOpen)

        viewModel.sendEvent(ExpensesEvent.OnDismissAddExpense)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isAddExpenseSheetOpen)
    }

    @Test
    fun `given manage stations clicked then emits NavigateToStations effect`() = runTest(testDispatcher) {
        val effects = mutableListOf<ExpensesEffect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        advanceUntilIdle()

        viewModel.sendEvent(ExpensesEvent.OnManageStationsClicked)
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(ExpensesEffect.NavigateToStations, effects.first())
    }

    @Test
    fun `given delete expense confirmed then calls deleteFuelExpenseUseCase`() = runTest(testDispatcher) {
        every { deleteFuelExpenseUseCase(any()) } returns flowOf(
            DeleteFuelExpenseUseCase.Output.Success
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(ExpensesEvent.OnDeleteExpense("exp-1"))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.showDeleteConfirmation)
        assertEquals("exp-1", viewModel.state.value.deleteTargetId)

        viewModel.sendEvent(ExpensesEvent.OnConfirmDeleteExpense)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.showDeleteConfirmation)
        assertNull(viewModel.state.value.deleteTargetId)
        coVerify { deleteFuelExpenseUseCase(DeleteFuelExpenseUseCase.Input("exp-1")) }
    }
}
