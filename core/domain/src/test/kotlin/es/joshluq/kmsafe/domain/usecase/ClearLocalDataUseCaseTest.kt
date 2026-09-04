package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.repository.PreferencesRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
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

class ClearLocalDataUseCaseTest {

    private val rentingRepository: RentingRepository = mockk()
    private val preferencesRepository: PreferencesRepository = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: ClearLocalDataUseCase

    @Before
    fun setUp() {
        useCase = ClearLocalDataUseCaseImpl(rentingRepository, preferencesRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `when invoke then clears database and preferences and emits Progress then Success`() = runTest {
        every { rentingRepository.clearAllLocalData() } returns flowOf(Unit)

        val emissions = useCase(ClearLocalDataUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is ClearLocalDataUseCase.Output.Progress)
        assertTrue(emissions[1] is ClearLocalDataUseCase.Output.Success)

        coVerify(exactly = 1) { rentingRepository.clearAllLocalData() }
        coVerify(exactly = 1) { preferencesRepository.clearPreferences() }
    }

    @Test
    fun `given error when invoke then catches and emits Failure`() = runTest {
        every { rentingRepository.clearAllLocalData() } returns flow { throw RuntimeException("Clear error") }

        val emissions = useCase(ClearLocalDataUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is ClearLocalDataUseCase.Output.Progress)
        assertTrue(emissions[1] is ClearLocalDataUseCase.Output.Failure)
    }
}
