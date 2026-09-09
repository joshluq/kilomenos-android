package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.model.AppOverlayState
import es.joshluq.kmsafe.domain.repository.AppOverlayRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ObserveAppOverlayUseCaseTest {

    private val repository: AppOverlayRepository = mockk()
    private lateinit var useCase: ObserveAppOverlayUseCase

    @Before
    fun setUp() {
        useCase = ObserveAppOverlayUseCaseImpl(repository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `when invoke then delegates to repository observeOverlay and emits Success`() = runTest {
        val expected = AppOverlayState.VehicleSwitching("Tesla Model 3")
        every { repository.observeOverlay() } returns flowOf(expected)

        val result = useCase(ObserveAppOverlayUseCase.Input).toList()

        assertEquals(1, result.size)
        assertEquals(ObserveAppOverlayUseCase.Output.Success(expected), result.first())
    }
}
