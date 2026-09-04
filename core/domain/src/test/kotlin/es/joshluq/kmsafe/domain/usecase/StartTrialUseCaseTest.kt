package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
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

class StartTrialUseCaseTest {

    private val repository: EntitlementsRepository = mockk()

    private lateinit var useCase: StartTrialUseCase

    @Before
    fun setUp() {
        useCase = StartTrialUseCaseImpl(repository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given deviceFingerprint when invoke then starts trial and emits Success`() = runTest {
        val trialEntitlements = Entitlements.Default.copy(
            subscriptionLevel = SubscriptionLevel.TRIAL,
            isTrialActive = true
        )
        every { repository.startTrial("fp-1") } returns flowOf(trialEntitlements)

        val emissions = useCase(StartTrialUseCase.Input("fp-1")).toList()

        assertEquals(1, emissions.size)
        val success = emissions[0] as StartTrialUseCase.Output.Success
        assertEquals(trialEntitlements, success.entitlements)
    }

    @Test
    fun `given already used trial when invoke then catches and emits Failure TrialAlreadyUsed`() = runTest {
        every { repository.startTrial("fp-1") } returns flow {
            throw KmException(KmError.TrialAlreadyUsed)
        }

        val emissions = useCase(StartTrialUseCase.Input("fp-1")).toList()

        assertEquals(1, emissions.size)
        val failure = emissions[0] as StartTrialUseCase.Output.Failure
        assertEquals(KmError.TrialAlreadyUsed, failure.error)
    }
}
