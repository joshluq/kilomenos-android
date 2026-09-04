package es.joshluq.kmsafe.feature.expenses.stations.detail

import androidx.lifecycle.SavedStateHandle
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.PriceTrend
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.domain.model.ServiceStationDetail
import es.joshluq.kmsafe.domain.model.StationPriceVolatility
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.GetServiceStationDetailUseCase
import es.joshluq.kmsafe.domain.usecase.GetStationVolatilityUseCase
import es.joshluq.kmsafe.domain.usecase.SetFavoriteStationUseCase
import es.joshluq.kmsafe.domain.usecase.SyncStationsUseCase
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StationDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getServiceStationDetailUseCase: GetServiceStationDetailUseCase = mockk()
    private val getStationVolatilityUseCase: GetStationVolatilityUseCase = mockk()
    private val setFavoriteStationUseCase: SetFavoriteStationUseCase = mockk()
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase = mockk()
    private val syncStationsUseCase: SyncStationsUseCase = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private val sampleStation = ServiceStation(
        id = "st-1",
        name = "Repsol Diagonal",
        brand = "Repsol",
        latitude = 41.3879,
        longitude = 2.1699,
        address = "Av Diagonal 123",
        isFavorite = false,
        availableEnergies = listOf(FuelType.GASOLINE_95)
    )

    private val sampleExpense = FuelExpense(
        id = "exp-1",
        vehicleId = "v1",
        stationId = "st-1",
        stationName = "Repsol Diagonal",
        timestamp = 1000L,
        fuelType = FuelType.GASOLINE_95,
        unitPrice = 1.60,
        volumeQuantity = 40.0,
        totalCost = 64.0
    )

    private val sampleDetail = ServiceStationDetail(
        station = sampleStation,
        totalSpent = 64.0,
        totalVolume = 40.0,
        refuelCount = 1,
        expenseHistory = listOf(sampleExpense),
        averageConsumption = 6.5
    )

    private val sampleVolatility = StationPriceVolatility(
        stationId = "st-1",
        fuelType = FuelType.GASOLINE_95,
        currentPrice = 1.60,
        historicalAveragePrice = 1.60,
        minRecordedPrice = 1.55,
        maxRecordedPrice = 1.65,
        priceTrend = PriceTrend.AVERAGE,
        priceHistory = emptyList()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { checkFeatureAccessUseCase(any()) } returns flowOf(
            CheckFeatureAccessUseCase.Output.Success(isGranted = true)
        )
        every { getServiceStationDetailUseCase(any()) } returns flowOf(
            GetServiceStationDetailUseCase.Output.Success(sampleDetail)
        )
        every { getStationVolatilityUseCase(any()) } returns flowOf(
            GetStationVolatilityUseCase.Output.Success(sampleVolatility)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun createViewModel(stationId: String = "st-1"): StationDetailViewModel {
        val handle = SavedStateHandle(mapOf("stationId" to stationId))
        return StationDetailViewModel(
            savedStateHandle = handle,
            getServiceStationDetailUseCase = getServiceStationDetailUseCase,
            getStationVolatilityUseCase = getStationVolatilityUseCase,
            setFavoriteStationUseCase = setFavoriteStationUseCase,
            checkFeatureAccessUseCase = checkFeatureAccessUseCase,
            syncStationsUseCase = syncStationsUseCase,
            logger = logger
        )
    }

    @Test
    fun `given station detail and volatility loaded when initialized then state is populated`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.isPremium)
        assertEquals("st-1", viewModel.state.value.detail?.station?.id)
        assertEquals(sampleDetail, viewModel.state.value.detail)
        assertEquals(sampleVolatility, viewModel.state.value.volatility)
    }

    @Test
    fun `given toggle favorite event then updates favorite state and calls use case`() = runTest(testDispatcher) {
        every { setFavoriteStationUseCase(SetFavoriteStationUseCase.Input("st-1", true)) } returns flowOf(
            SetFavoriteStationUseCase.Output.Success
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(StationDetailEvent.OnToggleFavorite(true))
        advanceUntilIdle()

        coVerify { setFavoriteStationUseCase(SetFavoriteStationUseCase.Input("st-1", true)) }
        assertEquals(true, viewModel.state.value.detail?.station?.isFavorite)
    }
}
