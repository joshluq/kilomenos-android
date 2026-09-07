package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.repository.AuthRepository
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import io.mockk.clearAllMocks
import io.mockk.coVerify
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

class SaveInitialContractUseCaseTest {

    private val rentingRepository: RentingRepository = mockk()
    private val historyRepository: HistoryRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk()
    private val entitlementsRepository: EntitlementsRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: SaveInitialContractUseCase

    @Before
    fun setUp() {
        every { rentingRepository.getAllContracts() } returns flowOf(emptyList())
        every { entitlementsRepository.observeEntitlements() } returns flowOf(Entitlements.Default)
        useCase = SaveInitialContractUseCaseImpl(
            rentingRepository = rentingRepository,
            historyRepository = historyRepository,
            authRepository = authRepository,
            entitlementsRepository = entitlementsRepository,
            logger = logger
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createContract(
        id: String = "contract-1",
        vehicleName: String = "Test Car",
        totalKms: Double = 10_000.0,
        durationMonths: Int = 12,
        startOdometer: Double = 5_000.0,
        currentOdometer: Double = 5_000.0
    ) = RentingContract(
        id = id,
        userId = "user-1",
        vehicleName = vehicleName,
        startDate = 1_000_000L,
        durationMonths = durationMonths,
        totalKms = totalKms,
        startOdometer = startOdometer,
        currentOdometer = currentOdometer
    )

    @Test
    fun `given unauthenticated user when invoke then emits Progress and Failure Unauthenticated`() = runTest {
        every { authRepository.getCurrentUser() } returns flowOf(null)

        val emissions = useCase(SaveInitialContractUseCase.Input(createContract())).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SaveInitialContractUseCase.Output.Progress)
        val failure = emissions[1] as SaveInitialContractUseCase.Output.Failure
        assertEquals(KmError.Unauthenticated, failure.error)
    }

    @Test
    fun `given blank vehicle name when invoke then emits Progress and Failure InvalidVehicleName`() = runTest {
        val user = User(id = "user-1", email = "test@example.com", name = "Test User")
        every { authRepository.getCurrentUser() } returns flowOf(user)

        val contract = createContract(vehicleName = "   ")
        val emissions = useCase(SaveInitialContractUseCase.Input(contract)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SaveInitialContractUseCase.Output.Progress)
        val failure = emissions[1] as SaveInitialContractUseCase.Output.Failure
        assertEquals(KmError.InvalidVehicleName, failure.error)
    }

    @Test
    fun `given invalid totalKms or durationMonths when invoke then emits Progress and Failure InvalidContractMetrics`() = runTest {
        val user = User(id = "user-1", email = "test@example.com", name = "Test User")
        every { authRepository.getCurrentUser() } returns flowOf(user)

        val contractWithZeroKms = createContract(totalKms = 0.0)
        val emissionsZeroKms = useCase(SaveInitialContractUseCase.Input(contractWithZeroKms)).toList()
        assertTrue((emissionsZeroKms[1] as SaveInitialContractUseCase.Output.Failure).error is KmError.InvalidContractMetrics)

        val contractWithZeroMonths = createContract(durationMonths = 0)
        val emissionsZeroMonths = useCase(SaveInitialContractUseCase.Input(contractWithZeroMonths)).toList()
        assertTrue((emissionsZeroMonths[1] as SaveInitialContractUseCase.Output.Failure).error is KmError.InvalidContractMetrics)
    }

    @Test
    fun `given valid contract without odometer offset when invoke then saves initial record and emits Success`() = runTest {
        val user = User(id = "user-1", email = "test@example.com", name = "Test User")
        val contract = createContract(startOdometer = 5_000.0, currentOdometer = 5_000.0)

        every { authRepository.getCurrentUser() } returns flowOf(user)
        every { rentingRepository.saveContract(any()) } returns flowOf("contract-1")
        every { rentingRepository.selectContract("contract-1") } returns flowOf(Unit)

        val emissions = useCase(SaveInitialContractUseCase.Input(contract)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SaveInitialContractUseCase.Output.Progress)
        val success = emissions[1] as SaveInitialContractUseCase.Output.Success
        assertEquals("contract-1", success.contractId)

        coVerify(exactly = 1) {
            historyRepository.saveRecord(match<OdometerRecord> {
                it.isInitialRecord && it.odometerValue == 5_000.0
            })
        }
        coVerify(exactly = 1) { rentingRepository.selectContract("contract-1") }
    }

    @Test
    fun `given valid contract with currentOdometer greater than startOdometer when invoke then creates offset record`() = runTest {
        val user = User(id = "user-1", email = "test@example.com", name = "Test User")
        val contract = createContract(startOdometer = 5_000.0, currentOdometer = 5_250.0)

        every { authRepository.getCurrentUser() } returns flowOf(user)
        every { rentingRepository.saveContract(any()) } returns flowOf("contract-1")
        every { rentingRepository.selectContract("contract-1") } returns flowOf(Unit)

        val emissions = useCase(SaveInitialContractUseCase.Input(contract)).toList()

        val success = emissions[1] as SaveInitialContractUseCase.Output.Success
        assertEquals("contract-1", success.contractId)

        coVerify(exactly = 1) {
            historyRepository.saveRecord(match<OdometerRecord> {
                it.isInitialRecord && it.odometerValue == 5_000.0
            })
        }
        coVerify(exactly = 1) {
            historyRepository.saveRecord(match<OdometerRecord> {
                !it.isInitialRecord && it.odometerValue == 250.0
            })
        }
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure UnknownError`() = runTest {
        val user = User(id = "user-1", email = "test@example.com", name = "Test User")
        every { authRepository.getCurrentUser() } returns flowOf(user)
        every { rentingRepository.saveContract(any()) } returns flow { throw RuntimeException("DB error") }

        val emissions = useCase(SaveInitialContractUseCase.Input(createContract())).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SaveInitialContractUseCase.Output.Progress)
        val failure = emissions[1] as SaveInitialContractUseCase.Output.Failure
        assertEquals(KmError.UnknownError, failure.error)
    }

    @Test
    fun `given existing contract and free tier without MULTI_VEHICLE when invoke then emits Failure MultiVehicleLimitReached`() = runTest {
        val user = User(id = "user-1", email = "test@example.com", name = "Test User")
        val existingContract = createContract(id = "existing-contract")
        every { authRepository.getCurrentUser() } returns flowOf(user)
        every { rentingRepository.getAllContracts() } returns flowOf(listOf(existingContract))
        every { entitlementsRepository.observeEntitlements() } returns flowOf(Entitlements.Default) // Default has no MULTI_VEHICLE

        val emissions = useCase(SaveInitialContractUseCase.Input(createContract(id = "new-contract"))).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SaveInitialContractUseCase.Output.Progress)
        val failure = emissions[1] as SaveInitialContractUseCase.Output.Failure
        assertEquals(KmError.MultiVehicleLimitReached, failure.error)
    }

    @Test
    fun `given existing contract and premium tier with MULTI_VEHICLE when invoke then allows saving and emits Success`() = runTest {
        val user = User(id = "user-1", email = "test@example.com", name = "Test User")
        val existingContract = createContract(id = "existing-contract")
        val premiumEntitlements = Entitlements.Default.copy(
            subscriptionLevel = SubscriptionLevel.PREMIUM
        )
        every { authRepository.getCurrentUser() } returns flowOf(user)
        every { rentingRepository.getAllContracts() } returns flowOf(listOf(existingContract))
        every { entitlementsRepository.observeEntitlements() } returns flowOf(premiumEntitlements)
        every { rentingRepository.saveContract(any()) } returns flowOf("new-contract")
        every { rentingRepository.selectContract("new-contract") } returns flowOf(Unit)

        val emissions = useCase(SaveInitialContractUseCase.Input(createContract(id = "new-contract"))).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SaveInitialContractUseCase.Output.Progress)
        val success = emissions[1] as SaveInitialContractUseCase.Output.Success
        assertEquals("new-contract", success.contractId)
    }
}
