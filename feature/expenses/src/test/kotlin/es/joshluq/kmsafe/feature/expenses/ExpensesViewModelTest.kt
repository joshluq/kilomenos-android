package es.joshluq.kmsafe.feature.expenses

import android.net.Uri
import es.joshluq.kmsafe.core.monetization.domain.MonetizationConfig
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.ArithmeticCheck
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.ReceiptScanResult
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.domain.model.StationRadarItem
import es.joshluq.kmsafe.domain.usecase.CalculateCostPerHundredKmUseCase
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteFuelExpenseUseCase
import es.joshluq.kmsafe.domain.usecase.DiscardReceiptScanUseCase
import es.joshluq.kmsafe.domain.usecase.GetAllServiceStationsUseCase
import es.joshluq.kmsafe.domain.usecase.GetElectrificationSavingsUseCase
import es.joshluq.kmsafe.domain.usecase.GetExpensesByVehicleUseCase
import es.joshluq.kmsafe.domain.usecase.GetStationVolatilityUseCase
import es.joshluq.kmsafe.domain.usecase.ObserveStationRadarUseCase
import es.joshluq.kmsafe.domain.usecase.ProcessFuelReceiptUseCase
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
    private val calculateCostPerHundredKmUseCase: CalculateCostPerHundredKmUseCase = mockk()
    private val observeStationRadarUseCase: ObserveStationRadarUseCase = mockk()
    private val processFuelReceiptUseCase: ProcessFuelReceiptUseCase = mockk()
    private val discardReceiptScanUseCase: DiscardReceiptScanUseCase = mockk()
    private val monetizationConfig: MonetizationConfig = mockk()
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
        every { monetizationConfig.getExpensesBannerAdUnitId() } returns "test_expenses_ad_unit_id"
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
        every { calculateCostPerHundredKmUseCase(any()) } returns flowOf(
            CalculateCostPerHundredKmUseCase.Output.Success(
                costPer100km = 8.50,
                lastCycleConsumption = 5.5,
                averageConsumption = 6.0,
                consumptionDeltaVsAverage = -8.33,
                totalFullTankKms = 500.0,
                totalFullTankCost = 42.50
            )
        )
        every { observeStationRadarUseCase(any()) } returns flowOf(
            ObserveStationRadarUseCase.Output.Success(
                items = listOf(
                    StationRadarItem(
                        station = ServiceStation(
                            id = "st-1",
                            name = "Repsol Center",
                            brand = "Repsol",
                            latitude = 41.3851,
                            longitude = 2.1734,
                            address = "Av. Diagonal 123"
                        ),
                        fuelType = FuelType.GASOLINE_95,
                        lastRecordedPrice = 1.65,
                        userAveragePrice = 1.70,
                        priceDelta = -0.05,
                        isOpportunity = true,
                        bestDayPrediction = "Lunes"
                    )
                ),
                isLocked = false
            )
        )
        every { discardReceiptScanUseCase(any()) } returns flowOf(DiscardReceiptScanUseCase.Output.Success)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun createViewModel(
        stationId: String? = null,
        autoOpenAdd: Boolean = false,
        priceReportMode: Boolean = false
    ): ExpensesViewModel {
        return ExpensesViewModel(
            initialStationId = stationId,
            autoOpenAdd = autoOpenAdd,
            priceReportMode = priceReportMode,
            getExpensesByVehicleUseCase = getExpensesByVehicleUseCase,
            saveFuelExpenseUseCase = saveFuelExpenseUseCase,
            deleteFuelExpenseUseCase = deleteFuelExpenseUseCase,
            getStationVolatilityUseCase = getStationVolatilityUseCase,
            getElectrificationSavingsUseCase = getElectrificationSavingsUseCase,
            checkFeatureAccessUseCase = checkFeatureAccessUseCase,
            getAllServiceStationsUseCase = getAllServiceStationsUseCase,
            saveServiceStationUseCase = saveServiceStationUseCase,
            calculateCostPerHundredKmUseCase = calculateCostPerHundredKmUseCase,
            observeStationRadarUseCase = observeStationRadarUseCase,
            processFuelReceiptUseCase = processFuelReceiptUseCase,
            discardReceiptScanUseCase = discardReceiptScanUseCase,
            monetizationConfig = monetizationConfig,
            logger = logger
        )
    }

    @Test
    fun `given initialized when created then state contains expenses adUnitId`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals("test_expenses_ad_unit_id", viewModel.state.value.adUnitId)
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

    @Test
    fun `given metrics and radar loaded then state contains costPer100km and radar items`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(8.50, viewModel.state.value.costPer100km ?: 0.0, 0.01)
        assertEquals(5.5, viewModel.state.value.lastCycleConsumption ?: 0.0, 0.01)
        assertEquals(-8.33, viewModel.state.value.consumptionDeltaVsAverage ?: 0.0, 0.01)
        assertEquals(1, viewModel.state.value.radarItems.size)
        assertEquals("st-1", viewModel.state.value.radarItems.first().station.id)
        assertFalse(viewModel.state.value.isRadarLocked)
    }

    @Test
    fun `given station radar selected then opens add expense sheet with initial station selected`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(ExpensesEvent.OnStationRadarSelected("st-1"))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isAddExpenseSheetOpen)
        assertEquals("st-1", viewModel.state.value.initialStationId)
    }

    @Test
    fun `given upgrade to unlock radar clicked then emits NavigateToUpgrade effect`() = runTest(testDispatcher) {
        val effects = mutableListOf<ExpensesEffect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        advanceUntilIdle()

        viewModel.sendEvent(ExpensesEvent.OnUpgradeToUnlockRadarClicked)
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(ExpensesEffect.NavigateToUpgrade, effects.first())
    }

    @Test
    fun `given receipt image captured when scanning succeeds then state contains scannedReceiptResult and opens sheet`() = runTest(testDispatcher) {
        val mockUri = mockk<Uri>()
        every { mockUri.toString() } returns "content://media/receipt_1.jpg"
        val mockResult = ReceiptScanResult(
            stationName = "Shell Express",
            purchaseDate = "2026-09-04T12:00:00Z",
            fuelType = FuelType.GASOLINE_95,
            liters = 40.0,
            pricePerLiter = 1.65,
            totalAmount = 66.0,
            currency = "EUR",
            confidenceScore = 0.95,
            isFuelReceipt = true,
            arithmeticCheck = ArithmeticCheck(
                valid = true,
                calculatedAmount = 66.0,
                discrepancy = 0.0
            ),
            storageFilePath = "receipts/receipt_1.jpg"
        )
        every { processFuelReceiptUseCase(any()) } returns flowOf(
            ProcessFuelReceiptUseCase.Output.Success(mockResult)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(ExpensesEvent.OnReceiptUriSelected(mockUri))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isScanningReceipt)
        assertTrue(viewModel.state.value.isAddExpenseSheetOpen)
        assertEquals("Shell Express", viewModel.state.value.scannedReceiptResult?.stationName)
        assertEquals("content://media/receipt_1.jpg", viewModel.state.value.receiptImagePath)
    }

    @Test
    fun `given receipt image captured when scanning fails then state has error and isScanningReceipt false`() = runTest(testDispatcher) {
        val mockUri = mockk<Uri>()
        every { mockUri.toString() } returns "content://media/receipt_2.jpg"
        every { processFuelReceiptUseCase(any()) } returns flowOf(
            ProcessFuelReceiptUseCase.Output.Failure(KmError.InvalidReceiptImage)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(ExpensesEvent.OnReceiptUriSelected(mockUri))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isScanningReceipt)
        assertNotNull(viewModel.state.value.error)
    }

    @Test
    fun `given free user when receipt uri selected then blocks scan and emits NavigateToUpgrade`() = runTest(testDispatcher) {
        every { checkFeatureAccessUseCase(any()) } returns flowOf(
            CheckFeatureAccessUseCase.Output.Success(isGranted = false)
        )
        val effects = mutableListOf<ExpensesEffect>()
        val mockUri = mockk<Uri>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isPremium)

        viewModel.sendEvent(ExpensesEvent.OnReceiptUriSelected(mockUri))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isScanningReceipt)
        assertNotNull(viewModel.state.value.error)
        assertEquals(1, effects.size)
        assertEquals(ExpensesEffect.NavigateToUpgrade, effects.first())
    }

    @Test
    fun `given receipt scanning fails with burst limit then state has error and isScanningReceipt false`() = runTest(testDispatcher) {
        val mockUri = mockk<Uri>()
        every { mockUri.toString() } returns "content://media/receipt_burst.jpg"
        every { processFuelReceiptUseCase(any()) } returns flowOf(
            ProcessFuelReceiptUseCase.Output.Failure(KmError.ReceiptScanRateLimitBurst(45))
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(ExpensesEvent.OnReceiptUriSelected(mockUri))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isScanningReceipt)
        assertNotNull(viewModel.state.value.error)
    }

    @Test
    fun `given receipt scanning fails with premium only from usecase then emits NavigateToUpgrade`() = runTest(testDispatcher) {
        val mockUri = mockk<Uri>()
        every { mockUri.toString() } returns "content://media/receipt_prem.jpg"
        every { processFuelReceiptUseCase(any()) } returns flowOf(
            ProcessFuelReceiptUseCase.Output.Failure(KmError.FuelExpensesPremiumOnly)
        )

        val effects = mutableListOf<ExpensesEffect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        advanceUntilIdle()

        viewModel.sendEvent(ExpensesEvent.OnReceiptUriSelected(mockUri))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isScanningReceipt)
        assertEquals(ExpensesEffect.NavigateToUpgrade, effects.first())
    }

    @Test
    fun `given discard receipt scan clicked then calls discardReceiptScanUseCase and clears scanned state`() = runTest(testDispatcher) {
        val mockUri = mockk<Uri>()
        every { mockUri.toString() } returns "content://media/receipt_3.jpg"
        val mockResult = ReceiptScanResult(
            stationName = "BP",
            purchaseDate = "2026-09-04T20:00:00Z",
            fuelType = FuelType.GASOLINE_95,
            liters = 30.0,
            pricePerLiter = 1.60,
            totalAmount = 48.0,
            currency = "EUR",
            confidenceScore = 0.92,
            isFuelReceipt = true,
            arithmeticCheck = ArithmeticCheck(
                valid = true,
                calculatedAmount = 48.0,
                discrepancy = 0.0
            ),
            storageFilePath = "receipts/receipt_3.jpg"
        )
        every { processFuelReceiptUseCase(any()) } returns flowOf(
            ProcessFuelReceiptUseCase.Output.Success(mockResult)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(ExpensesEvent.OnReceiptUriSelected(mockUri))
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.scannedReceiptResult)

        viewModel.sendEvent(ExpensesEvent.OnDiscardReceiptScan)
        advanceUntilIdle()

        assertNull(viewModel.state.value.scannedReceiptResult)
        assertNull(viewModel.state.value.receiptImagePath)
        coVerify { discardReceiptScanUseCase(DiscardReceiptScanUseCase.Input("receipts/receipt_3.jpg")) }
    }

    @Test
    fun `given save expense with receiptImagePath then passes receiptImagePath to SaveFuelExpenseUseCase`() = runTest(testDispatcher) {
        every { saveFuelExpenseUseCase(any()) } returns flowOf(
            SaveFuelExpenseUseCase.Output.Success("exp-new")
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(
            ExpensesEvent.OnSaveExpense(
                fuelType = FuelType.GASOLINE_95,
                unitPrice = 1.65,
                volumeQuantity = 40.0,
                totalCost = 66.0,
                stationId = "st-1",
                stationName = "Repsol Center",
                odometerAtExpense = 15500.0,
                isFullTank = true,
                notes = null,
                lastRefuelTimestamp = null,
                receiptImagePath = "/cache/receipt_saved.jpg"
            )
        )
        advanceUntilIdle()

        coVerify {
            saveFuelExpenseUseCase(
                match { input ->
                    input.receiptImagePath == "/cache/receipt_saved.jpg" &&
                            input.totalCost == 66.0
                }
            )
        }
        assertFalse(viewModel.state.value.isAddExpenseSheetOpen)
        assertNull(viewModel.state.value.receiptImagePath)
    }

    @Test
    fun `given save expense with null stationId but matching stationName then reuses existing stationId without creating new station`() = runTest(testDispatcher) {
        val existingStation = ServiceStation(
            id = "st-existing",
            name = "Repsol Diagonal",
            brand = "Repsol",
            latitude = 41.38,
            longitude = 2.17,
            address = "Diagonal 123"
        )
        every { getAllServiceStationsUseCase(any()) } returns flowOf(
            GetAllServiceStationsUseCase.Output.Success(listOf(existingStation))
        )
        every { saveFuelExpenseUseCase(any()) } returns flowOf(
            SaveFuelExpenseUseCase.Output.Success("exp-1")
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(
            ExpensesEvent.OnSaveExpense(
                fuelType = FuelType.GASOLINE_95,
                unitPrice = 1.65,
                volumeQuantity = 40.0,
                totalCost = 66.0,
                stationId = null,
                stationName = "  repsol diagonal  ",
                odometerAtExpense = 15500.0,
                isFullTank = true,
                notes = null,
                lastRefuelTimestamp = null,
                receiptImagePath = null
            )
        )
        advanceUntilIdle()

        coVerify(exactly = 0) { saveServiceStationUseCase(any()) }
        coVerify {
            saveFuelExpenseUseCase(
                match { input ->
                    input.stationId == "st-existing"
                }
            )
        }
    }

    @Test
    fun `given save expense with null stationId and unknown stationName then creates new station and saves expense`() = runTest(testDispatcher) {
        every { saveServiceStationUseCase(any()) } returns flowOf(
            SaveServiceStationUseCase.Output.Success("st-brand-new")
        )
        every { saveFuelExpenseUseCase(any()) } returns flowOf(
            SaveFuelExpenseUseCase.Output.Success("exp-2")
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(
            ExpensesEvent.OnSaveExpense(
                fuelType = FuelType.GASOLINE_95,
                unitPrice = 1.65,
                volumeQuantity = 40.0,
                totalCost = 66.0,
                stationId = null,
                stationName = "Gasolinera Nueva",
                odometerAtExpense = 15500.0,
                isFullTank = true,
                notes = null,
                lastRefuelTimestamp = null,
                receiptImagePath = null
            )
        )
        advanceUntilIdle()

        coVerify(exactly = 1) {
            saveServiceStationUseCase(
                match { input ->
                    input.name == "Gasolinera Nueva"
                }
            )
        }
        coVerify {
            saveFuelExpenseUseCase(
                match { input ->
                    input.stationId == "st-brand-new"
                }
            )
        }
    }
}
