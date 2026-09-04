package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.repository.AuthRepository
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

class SignInUseCaseTest {

    private val repository: AuthRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: SignInUseCase

    @Before
    fun setUp() {
        useCase = SignInUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given credentials when invoke then signs in and emits Progress then Success`() = runTest {
        val user = User(id = "u1", email = "test@example.com", name = "Test User")
        every { repository.signIn("test@example.com", "Password123!") } returns flowOf(user)

        val emissions = useCase(SignInUseCase.Input("test@example.com", "Password123!")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SignInUseCase.Output.Progress)
        val success = emissions[1] as SignInUseCase.Output.Success
        assertEquals(user, success.user)
    }

    @Test
    fun `given invalid credentials when invoke then catches KmException and emits Failure`() = runTest {
        every { repository.signIn("test@example.com", "WrongPassword") } returns flow {
            throw KmException(KmError.InvalidCredentials)
        }

        val emissions = useCase(SignInUseCase.Input("test@example.com", "WrongPassword")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SignInUseCase.Output.Progress)
        val failure = emissions[1] as SignInUseCase.Output.Failure
        assertEquals(KmError.InvalidCredentials, failure.error)
    }
}
