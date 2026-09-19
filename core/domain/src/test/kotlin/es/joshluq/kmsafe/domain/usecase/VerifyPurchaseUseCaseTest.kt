package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.VerifyPurchaseResult
import es.joshluq.kmsafe.domain.repository.BillingRepository
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

class VerifyPurchaseUseCaseTest {

    private val billingRepository: BillingRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: VerifyPurchaseUseCase

    @Before
    fun setUp() {
        useCase = VerifyPurchaseUseCaseImpl(billingRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given valid purchase when invoke then emits Progress and Success`() = runTest {
        val result = VerifyPurchaseResult(
            success = true,
            message = "Purchase verified",
            subscriptionStatus = "SUBSCRIPTION_STATE_ACTIVE"
        )
        every {
            billingRepository.verifyPurchase("test_token", "subscription_premium_monthly", null)
        } returns flowOf(result)

        val emissions = useCase(
            VerifyPurchaseUseCase.Input("test_token", "subscription_premium_monthly")
        ).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is VerifyPurchaseUseCase.Output.Progress)
        val success = emissions[1] as VerifyPurchaseUseCase.Output.Success
        assertEquals(result, success.result)
    }

    @Test
    fun `given rejected purchase when invoke then emits Progress and Failure`() = runTest {
        val result = VerifyPurchaseResult(
            success = false,
            message = "Invalid token"
        )
        every {
            billingRepository.verifyPurchase("bad_token", "subscription_premium_monthly", null)
        } returns flowOf(result)

        val emissions = useCase(
            VerifyPurchaseUseCase.Input("bad_token", "subscription_premium_monthly")
        ).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is VerifyPurchaseUseCase.Output.Progress)
        val failure = emissions[1] as VerifyPurchaseUseCase.Output.Failure
        assertEquals("Invalid token", failure.message)
    }

    @Test
    fun `given exception when invoke then emits Progress and Failure`() = runTest {
        every {
            billingRepository.verifyPurchase(any(), any(), any())
        } returns flow { throw RuntimeException("Network error") }

        val emissions = useCase(
            VerifyPurchaseUseCase.Input("token", "sku")
        ).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is VerifyPurchaseUseCase.Output.Progress)
        val failure = emissions[1] as VerifyPurchaseUseCase.Output.Failure
        assertEquals("Network error", failure.message)
    }
}
