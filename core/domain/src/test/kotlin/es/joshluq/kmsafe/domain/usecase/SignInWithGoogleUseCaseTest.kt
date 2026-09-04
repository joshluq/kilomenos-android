package es.joshluq.kmsafe.domain.usecase

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

class SignInWithGoogleUseCaseTest {

    private val repository: AuthRepository = mockk()

    private lateinit var useCase: SignInWithGoogleUseCase

    @Before
    fun setUp() {
        useCase = SignInWithGoogleUseCaseImpl(repository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given valid idToken when invoke then signs in and emits Progress then Success`() = runTest {
        val user = User(id = "u1", email = "google@example.com", name = "Google User")
        every { repository.signInWithGoogle("valid-token") } returns flowOf(user)

        val emissions = useCase(SignInWithGoogleUseCase.Input("valid-token")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SignInWithGoogleUseCase.Output.Progress)
        val success = emissions[1] as SignInWithGoogleUseCase.Output.Success
        assertEquals(user, success.user)
    }

    @Test
    fun `given invalid idToken when invoke then catches and emits Failure`() = runTest {
        every { repository.signInWithGoogle("invalid-token") } returns flow {
            throw KmException(KmError.InvalidCredentials)
        }

        val emissions = useCase(SignInWithGoogleUseCase.Input("invalid-token")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SignInWithGoogleUseCase.Output.Progress)
        val failure = emissions[1] as SignInWithGoogleUseCase.Output.Failure
        assertEquals(KmError.InvalidCredentials, failure.error)
    }
}
