package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.AuthSessionState
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.repository.AuthRepository
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

class CheckSessionUseCaseTest {

    private val repository: AuthRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: CheckSessionUseCase

    @Before
    fun setUp() {
        useCase = CheckSessionUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given session state Initializing when invoke then emits Progress`() = runTest {
        every { repository.getSessionState() } returns flowOf(AuthSessionState.Initializing)
        every { repository.getCurrentUser() } returns flowOf(null)

        val emissions = useCase(CheckSessionUseCase.Input).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0] is CheckSessionUseCase.Output.Progress)
    }

    @Test
    fun `given session state Idle when invoke then emits IdleSession`() = runTest {
        every { repository.getSessionState() } returns flowOf(AuthSessionState.Idle)
        every { repository.getCurrentUser() } returns flowOf(null)

        val emissions = useCase(CheckSessionUseCase.Input).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0] is CheckSessionUseCase.Output.IdleSession)
    }

    @Test
    fun `given session state Active with user when invoke then emits ActiveSession`() = runTest {
        val user = User(id = "u1", email = "test@example.com", name = "Test User")
        every { repository.getSessionState() } returns flowOf(AuthSessionState.Active)
        every { repository.getCurrentUser() } returns flowOf(user)

        val emissions = useCase(CheckSessionUseCase.Input).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0] is CheckSessionUseCase.Output.ActiveSession)
    }

    @Test
    fun `given session state Active but user is null when invoke then emits InconsistentSession`() = runTest {
        every { repository.getSessionState() } returns flowOf(AuthSessionState.Active)
        every { repository.getCurrentUser() } returns flowOf(null)

        val emissions = useCase(CheckSessionUseCase.Input).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0] is CheckSessionUseCase.Output.InconsistentSession)
    }
}
