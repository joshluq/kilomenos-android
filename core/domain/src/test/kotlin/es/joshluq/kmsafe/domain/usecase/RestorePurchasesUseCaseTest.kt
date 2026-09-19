package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
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

class RestorePurchasesUseCaseTest {

    private val billingRepository: BillingRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: RestorePurchasesUseCase

    @Before
    fun setUp() {
        useCase = RestorePurchasesUseCaseImpl(billingRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given active purchases when invoke then emits Progress and Success with count`() = runTest {
        every { billingRepository.restorePurchases() } returns flowOf(2)

        val emissions = useCase(RestorePurchasesUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is RestorePurchasesUseCase.Output.Progress)
        val success = emissions[1] as RestorePurchasesUseCase.Output.Success
        assertEquals(2, success.restoredCount)
    }

    @Test
    fun `given no active purchases when invoke then emits Progress and NoPurchasesFound`() = runTest {
        every { billingRepository.restorePurchases() } returns flowOf(0)

        val emissions = useCase(RestorePurchasesUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is RestorePurchasesUseCase.Output.Progress)
        assertTrue(emissions[1] is RestorePurchasesUseCase.Output.NoPurchasesFound)
    }

    @Test
    fun `given error when invoke then emits Progress and Failure`() = runTest {
        every { billingRepository.restorePurchases() } returns flow {
            throw RuntimeException("Google Play error")
        }

        val emissions = useCase(RestorePurchasesUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is RestorePurchasesUseCase.Output.Progress)
        val failure = emissions[1] as RestorePurchasesUseCase.Output.Failure
        assertEquals("Google Play error", failure.message)
    }
}
