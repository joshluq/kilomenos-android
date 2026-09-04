package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.model.Entitlements
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
import org.junit.Before
import org.junit.Test

class GetEntitlementsUseCaseTest {

    private val repository: EntitlementsRepository = mockk()

    private lateinit var useCase: GetEntitlementsUseCase

    @Before
    fun setUp() {
        useCase = GetEntitlementsUseCaseImpl(repository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given forceRefresh false when invoke then observes entitlements and emits Success`() = runTest {
        val entitlements = Entitlements.Default.copy(subscriptionLevel = SubscriptionLevel.PREMIUM)
        every { repository.observeEntitlements() } returns flowOf(entitlements)

        val emissions = useCase(GetEntitlementsUseCase.Input(deviceFingerprint = "fingerprint-1", forceRefresh = false)).toList()

        assertEquals(1, emissions.size)
        val success = emissions[0] as GetEntitlementsUseCase.Output.Success
        assertEquals(entitlements, success.entitlements)
    }

    @Test
    fun `given forceRefresh true when invoke then queries remote entitlements and emits Success`() = runTest {
        val entitlements = Entitlements.Default.copy(subscriptionLevel = SubscriptionLevel.PREMIUM)
        every { repository.getEntitlements("fingerprint-1", forceRefresh = true) } returns flowOf(entitlements)

        val emissions = useCase(GetEntitlementsUseCase.Input(deviceFingerprint = "fingerprint-1", forceRefresh = true)).toList()

        assertEquals(1, emissions.size)
        val success = emissions[0] as GetEntitlementsUseCase.Output.Success
        assertEquals(entitlements, success.entitlements)
    }
}
