package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.ArithmeticCheck
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.ReceiptScanResult
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.repository.AppOverlayRepository
import es.joshluq.kmsafe.domain.repository.AuthRepository
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import es.joshluq.kmsafe.domain.repository.ReceiptRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProcessFuelReceiptUseCaseTest {

    private val receiptRepository: ReceiptRepository = mockk()
    private val authRepository: AuthRepository = mockk()
    private val entitlementsRepository: EntitlementsRepository = mockk()
    private val appOverlayRepository: AppOverlayRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: ProcessFuelReceiptUseCase

    private val sampleUser = User(id = "user-1", email = "test@example.com", name = "Test")
    private val sampleScanResult = ReceiptScanResult(
        stationName = "Repsol",
        purchaseDate = "2026-08-25T14:30:00Z",
        fuelType = FuelType.DIESEL,
        liters = 40.0,
        pricePerLiter = 1.5,
        totalAmount = 60.0,
        confidenceScore = 0.95,
        isFuelReceipt = true,
        arithmeticCheck = ArithmeticCheck(valid = true, calculatedAmount = 60.0, discrepancy = 0.0),
        storageFilePath = "user-1/receipt.jpg"
    )

    @Before
    fun setUp() {
        coEvery { appOverlayRepository.setOverlay(any()) } just Runs
        coEvery { appOverlayRepository.clearOverlay() } just Runs
        useCase = ProcessFuelReceiptUseCaseImpl(
            receiptRepository = receiptRepository,
            authRepository = authRepository,
            entitlementsRepository = entitlementsRepository,
            appOverlayRepository = appOverlayRepository,
            logger = logger
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given unauthenticated user when invoke then emits Failure Unauthenticated`() = runTest {
        every { authRepository.getCurrentUser() } returns flowOf(null)

        val emissions = useCase(ProcessFuelReceiptUseCase.Input("content://media/receipt.jpg", "veh-1")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is ProcessFuelReceiptUseCase.Output.Progress)
        val failure = emissions[1] as ProcessFuelReceiptUseCase.Output.Failure
        assertEquals(KmError.Unauthenticated, failure.error)
    }

    @Test
    fun `given free user without active trial when invoke then emits Failure FuelExpensesPremiumOnly`() = runTest {
        every { authRepository.getCurrentUser() } returns flowOf(sampleUser)
        every { entitlementsRepository.observeEntitlements() } returns flowOf(Entitlements.Default) // FREE tier, isTrialActive = false

        val emissions = useCase(ProcessFuelReceiptUseCase.Input("content://media/receipt.jpg", "veh-1")).toList()

        assertEquals(2, emissions.size)
        val failure = emissions[1] as ProcessFuelReceiptUseCase.Output.Failure
        assertEquals(KmError.FuelExpensesPremiumOnly, failure.error)
    }

    @Test
    fun `given active trial user when invoke then uploads and extracts receipt successfully`() = runTest {
        val trialEntitlements = Entitlements.Default.copy(
            subscriptionLevel = SubscriptionLevel.TRIAL,
            isTrialActive = true
        )
        every { authRepository.getCurrentUser() } returns flowOf(sampleUser)
        every { entitlementsRepository.observeEntitlements() } returns flowOf(trialEntitlements)
        every { receiptRepository.uploadReceiptFromUri(sampleUser.id, any(), any()) } returns flowOf("user-1/receipt.jpg")
        every { receiptRepository.processReceipt("user-1/receipt.jpg") } returns flowOf(sampleScanResult)

        val emissions = useCase(ProcessFuelReceiptUseCase.Input("content://media/receipt.jpg", "veh-1")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is ProcessFuelReceiptUseCase.Output.Progress)
        val success = emissions[1] as ProcessFuelReceiptUseCase.Output.Success
        assertEquals("Repsol", success.result.stationName)
        assertEquals(60.0, success.result.totalAmount, 0.001)
    }

    @Test
    fun `given valid image and premium user when invoke then uploads and extracts receipt successfully`() = runTest {
        val premiumEntitlements = Entitlements.Default.copy(subscriptionLevel = SubscriptionLevel.PREMIUM)
        every { authRepository.getCurrentUser() } returns flowOf(sampleUser)
        every { entitlementsRepository.observeEntitlements() } returns flowOf(premiumEntitlements)
        every { receiptRepository.uploadReceiptFromUri(sampleUser.id, any(), any()) } returns flowOf("user-1/receipt.jpg")
        every { receiptRepository.processReceipt("user-1/receipt.jpg") } returns flowOf(sampleScanResult)

        val emissions = useCase(ProcessFuelReceiptUseCase.Input("content://media/receipt.jpg", "veh-1")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is ProcessFuelReceiptUseCase.Output.Progress)
        val success = emissions[1] as ProcessFuelReceiptUseCase.Output.Success
        assertEquals("Repsol", success.result.stationName)
        assertEquals(60.0, success.result.totalAmount, 0.001)
    }

    @Test
    fun `given document is not a fuel receipt when invoke then deletes uploaded image and emits InvalidReceiptImage`() = runTest {
        val nonFuelReceipt = sampleScanResult.copy(isFuelReceipt = false, stationName = "Mercadona")
        every { authRepository.getCurrentUser() } returns flowOf(sampleUser)
        every { entitlementsRepository.observeEntitlements() } returns flowOf(Entitlements.Default.copy(subscriptionLevel = SubscriptionLevel.PREMIUM))
        every { receiptRepository.uploadReceiptFromUri(sampleUser.id, any(), any()) } returns flowOf("user-1/receipt.jpg")
        every { receiptRepository.processReceipt("user-1/receipt.jpg") } returns flowOf(nonFuelReceipt)
        every { receiptRepository.deleteReceiptImage("user-1/receipt.jpg") } returns flowOf(Unit)

        val emissions = useCase(ProcessFuelReceiptUseCase.Input("content://media/receipt.jpg", "veh-1")).toList()

        assertEquals(2, emissions.size)
        val failure = emissions[1] as ProcessFuelReceiptUseCase.Output.Failure
        assertEquals(KmError.InvalidReceiptImage, failure.error)
        coVerify(exactly = 1) { receiptRepository.deleteReceiptImage("user-1/receipt.jpg") }
    }
}
