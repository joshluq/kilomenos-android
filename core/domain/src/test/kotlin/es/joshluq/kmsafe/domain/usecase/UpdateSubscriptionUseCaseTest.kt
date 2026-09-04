package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
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

class UpdateSubscriptionUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: UpdateSubscriptionUseCase

    @Before
    fun setUp() {
        useCase = UpdateSubscriptionUseCaseImpl(authRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given new subscription level when invoke then updates subscription and emits Progress then Success`() = runTest {
        val user = User(id = "u1", email = "test@example.com", name = "User")
        every { authRepository.updateSubscription(SubscriptionLevel.PREMIUM) } returns flowOf(user)

        val emissions = useCase(UpdateSubscriptionUseCase.Input(SubscriptionLevel.PREMIUM)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is UpdateSubscriptionUseCase.Output.Progress)
        val success = emissions[1] as UpdateSubscriptionUseCase.Output.Success
        assertEquals(user, success.user)
    }

    @Test
    fun `given error when invoke then catches and emits Failure`() = runTest {
        every { authRepository.updateSubscription(SubscriptionLevel.PREMIUM) } returns flow {
            throw RuntimeException("Billing update failed")
        }

        val emissions = useCase(UpdateSubscriptionUseCase.Input(SubscriptionLevel.PREMIUM)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is UpdateSubscriptionUseCase.Output.Progress)
        assertTrue(emissions[1] is UpdateSubscriptionUseCase.Output.Failure)
    }
}
