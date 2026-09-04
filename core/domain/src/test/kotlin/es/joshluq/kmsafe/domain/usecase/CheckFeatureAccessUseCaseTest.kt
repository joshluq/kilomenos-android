package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CheckFeatureAccessUseCaseTest {

    private val repository: EntitlementsRepository = mockk()

    private lateinit var useCase: CheckFeatureAccessUseCase

    @Before
    fun setUp() {
        useCase = CheckFeatureAccessUseCaseImpl(repository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given premium user when invoke then returns isGranted true`() = runTest {
        val entitlements = Entitlements.Default.copy(subscriptionLevel = SubscriptionLevel.PREMIUM)
        every { repository.observeEntitlements() } returns flowOf(entitlements)

        val emissions = useCase(CheckFeatureAccessUseCase.Input(Feature.MULTI_VEHICLE)).toList()

        assertEquals(1, emissions.size)
        val success = emissions[0] as CheckFeatureAccessUseCase.Output.Success
        assertTrue(success.isGranted)
    }

    @Test
    fun `given free user when invoke then returns isGranted false`() = runTest {
        val entitlements = Entitlements.Default.copy(subscriptionLevel = SubscriptionLevel.FREE)
        every { repository.observeEntitlements() } returns flowOf(entitlements)

        val emissions = useCase(CheckFeatureAccessUseCase.Input(Feature.MULTI_VEHICLE)).toList()

        assertEquals(1, emissions.size)
        val success = emissions[0] as CheckFeatureAccessUseCase.Output.Success
        assertFalse(success.isGranted)
    }
}
