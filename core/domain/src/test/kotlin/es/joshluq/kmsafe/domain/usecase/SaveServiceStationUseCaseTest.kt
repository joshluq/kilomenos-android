package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import io.mockk.clearAllMocks
import io.mockk.coVerify
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

class SaveServiceStationUseCaseTest {

    private val repository: ServiceStationRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: SaveServiceStationUseCase

    @Before
    fun setUp() {
        useCase = SaveServiceStationUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given input when invoke then saves station and emits Progress then Success`() = runTest {
        val input = SaveServiceStationUseCase.Input(
            id = "st-1",
            name = "Repsol Madrid",
            brand = "Repsol",
            latitude = 40.4,
            longitude = -3.7,
            address = "Calle Mayor 1",
            isFavorite = true,
            availableEnergies = listOf(FuelType.GASOLINE_95)
        )

        every { repository.saveStation(any()) } returns flowOf("st-1")

        val emissions = useCase(input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SaveServiceStationUseCase.Output.Progress)
        val success = emissions[1] as SaveServiceStationUseCase.Output.Success
        assertEquals("st-1", success.stationId)

        coVerify(exactly = 1) {
            repository.saveStation(match {
                it.id == "st-1" && it.name == "Repsol Madrid" && it.isFavorite
            })
        }
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        val input = SaveServiceStationUseCase.Input(
            name = "Repsol",
            brand = "Repsol",
            latitude = 40.0,
            longitude = -3.0,
            address = "Address"
        )

        every { repository.saveStation(any()) } returns flow { throw RuntimeException("Error saving") }

        val emissions = useCase(input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SaveServiceStationUseCase.Output.Progress)
        assertTrue(emissions[1] is SaveServiceStationUseCase.Output.Failure)
    }
}
