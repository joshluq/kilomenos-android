package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.repository.AuthRepository
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

class SignOutUseCaseTest {

    private val repository: AuthRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: SignOutUseCase

    @Before
    fun setUp() {
        useCase = SignOutUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `when invoke with clearLocalData true then signs out and emits Progress then Success`() = runTest {
        every { repository.signOut(true) } returns flowOf(Unit)

        val emissions = useCase(SignOutUseCase.Input(clearLocalData = true)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SignOutUseCase.Output.Progress)
        assertTrue(emissions[1] is SignOutUseCase.Output.Success)

        coVerify(exactly = 1) { repository.signOut(true) }
    }

    @Test
    fun `given error when invoke then catches and emits Failure`() = runTest {
        every { repository.signOut(false) } returns flow { throw RuntimeException("Logout failed") }

        val emissions = useCase(SignOutUseCase.Input(clearLocalData = false)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SignOutUseCase.Output.Progress)
        val failure = emissions[1] as SignOutUseCase.Output.Failure
        assertEquals("Logout failed", failure.message)
    }
}
