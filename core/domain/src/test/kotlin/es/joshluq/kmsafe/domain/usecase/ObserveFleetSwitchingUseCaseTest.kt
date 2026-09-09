package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.model.FleetSwitchingState
import es.joshluq.kmsafe.domain.repository.RentingRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ObserveFleetSwitchingUseCaseTest {

    private val rentingRepository: RentingRepository = mockk()

    private lateinit var useCase: ObserveFleetSwitchingUseCase

    @Before
    fun setUp() {
        useCase = ObserveFleetSwitchingUseCaseImpl(rentingRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given switching state stream when invoke then maps to Success output`() = runTest {
        val expectedState = FleetSwitchingState(isSwitching = true, vehicleName = "Peugeot 3008")
        every { rentingRepository.observeFleetSwitching() } returns flowOf(expectedState)

        val emissions = useCase(ObserveFleetSwitchingUseCase.Input).toList()

        assertEquals(1, emissions.size)
        val output = emissions[0] as ObserveFleetSwitchingUseCase.Output.Success
        assertTrue(output.state.isSwitching)
        assertEquals("Peugeot 3008", output.state.vehicleName)
    }
}
