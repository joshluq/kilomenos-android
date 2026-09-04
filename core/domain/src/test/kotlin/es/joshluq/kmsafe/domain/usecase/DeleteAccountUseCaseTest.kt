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

class DeleteAccountUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: DeleteAccountUseCase

    @Before
    fun setUp() {
        useCase = DeleteAccountUseCaseImpl(authRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `when invoke then deletes account and emits Progress then Success`() = runTest {
        every { authRepository.deleteAccount() } returns flowOf(Unit)

        val emissions = useCase(DeleteAccountUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is DeleteAccountUseCase.Output.Progress)
        assertTrue(emissions[1] is DeleteAccountUseCase.Output.Success)

        coVerify(exactly = 1) { authRepository.deleteAccount() }
    }

    @Test
    fun `given error when invoke then catches and emits Failure`() = runTest {
        every { authRepository.deleteAccount() } returns flow { throw RuntimeException("Deletion failed") }

        val emissions = useCase(DeleteAccountUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is DeleteAccountUseCase.Output.Progress)
        assertTrue(emissions[1] is DeleteAccountUseCase.Output.Failure)
    }
}
