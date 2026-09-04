package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
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

class GetServiceStationDetailUseCaseTest {

    private val stationRepository: ServiceStationRepository = mockk()
    private val expenseRepository: FuelExpenseRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: GetServiceStationDetailUseCase

    @Before
    fun setUp() {
        useCase = GetServiceStationDetailUseCaseImpl(stationRepository, expenseRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createStation(id: String) = ServiceStation(
        id = id,
        name = "Station 1",
        brand = "Cepsa",
        latitude = 40.0,
        longitude = -3.0,
        address = "Address 1",
        isFavorite = true,
        availableEnergies = listOf(FuelType.DIESEL)
    )

    private fun createExpense(id: String, stationId: String, cost: Double, volume: Double, consumption: Double? = null) = FuelExpense(
        id = id,
        vehicleId = "v1",
        stationId = stationId,
        timestamp = 1000L,
        fuelType = FuelType.DIESEL,
        unitPrice = 1.4,
        volumeQuantity = volume,
        totalCost = cost,
        consumptionPer100km = consumption
    )

    @Test
    fun `given non-existing stationId when invoke then emits Progress and Failure`() = runTest {
        every { stationRepository.getStationById("missing") } returns flowOf(null)

        val emissions = useCase(GetServiceStationDetailUseCase.Input("missing")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetServiceStationDetailUseCase.Output.Progress)
        val failure = emissions[1] as GetServiceStationDetailUseCase.Output.Failure
        assertEquals(KmError.UnknownError, failure.error)
    }

    @Test
    fun `given station exists and has expenses when invoke then emits Progress and Success with aggregate details`() = runTest {
        val station = createStation("st-1")
        val exp1 = createExpense("e1", station.id, 50.0, 30.0, 6.0)
        val exp2 = createExpense("e2", station.id, 70.0, 40.0, 8.0)

        every { stationRepository.getStationById(station.id) } returns flowOf(station)
        every { expenseRepository.getExpensesByStation(station.id) } returns flowOf(listOf(exp1, exp2))

        val emissions = useCase(GetServiceStationDetailUseCase.Input(station.id)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetServiceStationDetailUseCase.Output.Progress)
        val success = emissions[1] as GetServiceStationDetailUseCase.Output.Success
        val detail = success.detail

        assertEquals(station, detail.station)
        assertEquals(120.0, detail.totalSpent, 0.001)
        assertEquals(70.0, detail.totalVolume, 0.001)
        assertEquals(2, detail.refuelCount)
        assertEquals(7.0, detail.averageConsumption!!, 0.001)
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        every { stationRepository.getStationById("st-1") } returns flow { throw RuntimeException("DB error") }

        val emissions = useCase(GetServiceStationDetailUseCase.Input("st-1")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetServiceStationDetailUseCase.Output.Progress)
        val failure = emissions[1] as GetServiceStationDetailUseCase.Output.Failure
        assertEquals(KmError.UnknownError, failure.error)
    }
}
