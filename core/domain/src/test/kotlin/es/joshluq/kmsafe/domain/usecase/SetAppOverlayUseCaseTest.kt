package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.model.AppOverlayState
import es.joshluq.kmsafe.domain.repository.AppOverlayRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SetAppOverlayUseCaseTest {

    private val repository: AppOverlayRepository = mockk()
    private lateinit var useCase: SetAppOverlayUseCase

    @Before
    fun setUp() {
        useCase = SetAppOverlayUseCaseImpl(repository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given non-none state when invoke then calls repository setOverlay and emits Success`() = runTest {
        val state = AppOverlayState.LoggingOut
        coEvery { repository.setOverlay(state) } just Runs

        val result = useCase(SetAppOverlayUseCase.Input(state)).toList()

        assertEquals(1, result.size)
        assertEquals(SetAppOverlayUseCase.Output.Success, result.first())
        coVerify(exactly = 1) { repository.setOverlay(state) }
    }

    @Test
    fun `given None state when invoke then calls repository clearOverlay and emits Success`() = runTest {
        coEvery { repository.clearOverlay() } just Runs

        val result = useCase(SetAppOverlayUseCase.Input(AppOverlayState.None)).toList()

        assertEquals(1, result.size)
        assertEquals(SetAppOverlayUseCase.Output.Success, result.first())
        coVerify(exactly = 1) { repository.clearOverlay() }
    }
}
