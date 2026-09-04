package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.PriceTrend
import es.joshluq.kmsafe.domain.model.StationPriceVolatility
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetStationVolatilityUseCaseTest {

    private val repository: ServiceStationRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: GetStationVolatilityUseCase

    @Before
    fun setUp() {
        useCase = GetStationVolatilityUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createVolatility() = StationPriceVolatility(
        stationId = "st-1",
        fuelType = FuelType.GASOLINE_95,
        currentPrice = 1.55,
        historicalAveragePrice = 1.60,
        minRecordedPrice = 1.50,
        maxRecordedPrice = 1.70,
        priceTrend = PriceTrend.CHEAPER
    )

    @Test
    fun `given volatility found when invoke then emits Progress and Success`() = runTest {
        val volatility = createVolatility()
        every { repository.getStationVolatility("st-1", FuelType.GASOLINE_95) } returns flowOf(volatility)

        val emissions = useCase(GetStationVolatilityUseCase.Input("st-1", FuelType.GASOLINE_95)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetStationVolatilityUseCase.Output.Progress)
        val success = emissions[1] as GetStationVolatilityUseCase.Output.Success
        assertEquals(volatility, success.volatility)
    }

    @Test
    fun `given volatility null when invoke then emits Progress and Empty`() = runTest {
        every { repository.getStationVolatility("st-1", FuelType.GASOLINE_95) } returns flowOf(null)

        val emissions = useCase(GetStationVolatilityUseCase.Input("st-1", FuelType.GASOLINE_95)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetStationVolatilityUseCase.Output.Progress)
        assertTrue(emissions[1] is GetStationVolatilityUseCase.Output.Empty)
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        every { repository.getStationVolatility("st-1", FuelType.GASOLINE_95) } returns flow { throw RuntimeException("Error") }

        val emissions = useCase(GetStationVolatilityUseCase.Input("st-1", FuelType.GASOLINE_95)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetStationVolatilityUseCase.Output.Progress)
        assertTrue(emissions[1] is GetStationVolatilityUseCase.Output.Failure)
    }
}
