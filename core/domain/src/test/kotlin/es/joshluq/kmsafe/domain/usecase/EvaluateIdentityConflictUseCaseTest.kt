package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.UserPreferences
import es.joshluq.kmsafe.domain.repository.PreferencesRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
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

class EvaluateIdentityConflictUseCaseTest {

    private val rentingRepository: RentingRepository = mockk()
    private val preferencesRepository: PreferencesRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: EvaluateIdentityConflictUseCase

    @Before
    fun setUp() {
        useCase = EvaluateIdentityConflictUseCaseImpl(rentingRepository, preferencesRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given same email as last session when invoke then emits Progress and NoConflict`() = runTest {
        coEvery { rentingRepository.getDatabaseOwnerId() } returns "user-1"
        coEvery { rentingRepository.hasLocalData() } returns true
        every { preferencesRepository.getGlobalPreferences() } returns flowOf(UserPreferences(lastEmail = "user@test.com"))

        val emissions = useCase(EvaluateIdentityConflictUseCase.Input("user@test.com")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is EvaluateIdentityConflictUseCase.Output.Progress)
        assertTrue(emissions[1] is EvaluateIdentityConflictUseCase.Output.NoConflict)
    }

    @Test
    fun `given different email and hasLocalData true when invoke then emits Progress and ShowWarning`() = runTest {
        coEvery { rentingRepository.getDatabaseOwnerId() } returns "user-1"
        coEvery { rentingRepository.hasLocalData() } returns true
        every { preferencesRepository.getGlobalPreferences() } returns flowOf(UserPreferences(lastEmail = "old@test.com"))

        val emissions = useCase(EvaluateIdentityConflictUseCase.Input("new@test.com")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is EvaluateIdentityConflictUseCase.Output.Progress)
        assertTrue(emissions[1] is EvaluateIdentityConflictUseCase.Output.ShowWarning)
    }

    @Test
    fun `given different email and hasLocalData false when invoke then emits Progress and SilentCleanup`() = runTest {
        coEvery { rentingRepository.getDatabaseOwnerId() } returns null
        coEvery { rentingRepository.hasLocalData() } returns false
        every { preferencesRepository.getGlobalPreferences() } returns flowOf(UserPreferences(lastEmail = "old@test.com"))

        val emissions = useCase(EvaluateIdentityConflictUseCase.Input("new@test.com")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is EvaluateIdentityConflictUseCase.Output.Progress)
        assertTrue(emissions[1] is EvaluateIdentityConflictUseCase.Output.SilentCleanup)
    }
}
