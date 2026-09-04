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

class SignUpUseCaseTest {

    private val repository: AuthRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: SignUpUseCase

    @Before
    fun setUp() {
        useCase = SignUpUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given new user data when invoke then signs up and emits Progress then Success`() = runTest {
        val user = User(id = "u1", email = "new@example.com", name = "New User")
        every { repository.signUp("new@example.com", "Password123!", "New User") } returns flowOf(user)

        val emissions = useCase(SignUpUseCase.Input("new@example.com", "Password123!", "New User")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SignUpUseCase.Output.Progress)
        val success = emissions[1] as SignUpUseCase.Output.Success
        assertEquals(user, success.user)
    }

    @Test
    fun `given existing email when invoke then catches and emits Failure`() = runTest {
        every { repository.signUp("exists@example.com", "Password123!", "User") } returns flow {
            throw KmException(KmError.UserAlreadyRegistered)
        }

        val emissions = useCase(SignUpUseCase.Input("exists@example.com", "Password123!", "User")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SignUpUseCase.Output.Progress)
        val failure = emissions[1] as SignUpUseCase.Output.Failure
        assertEquals(KmError.UserAlreadyRegistered, failure.error)
    }
}
