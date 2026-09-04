package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.TripRoute
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

class AddOdometerRecordUseCaseTest {

    private val rentingRepository: RentingRepository = mockk()
    private val historyRepository: HistoryRepository = mockk(relaxed = true)

    private lateinit var useCase: AddOdometerRecordUseCase

    @Before
    fun setUp() {
        useCase = AddOdometerRecordUseCaseImpl(rentingRepository, historyRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createContract() = RentingContract(
        id = "contract-1",
        userId = "user-1",
        vehicleName = "Car",
        startDate = 1000L,
        durationMonths = 12,
        totalKms = 10000.0,
        startOdometer = 0.0,
        currentOdometer = 0.0
    )

    @Test
    fun `given no contract when invoke then emits Progress and Failure`() = runTest {
        every { rentingRepository.getContract() } returns flowOf(null)

        val emissions = useCase(AddOdometerRecordUseCase.Input(odometerValue = 50.0, timestamp = 2000L)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is AddOdometerRecordUseCase.Output.Progress)
        val failure = emissions[1] as AddOdometerRecordUseCase.Output.Failure
        assertEquals("No renting contract found", failure.message)
    }

    @Test
    fun `given contract and input without route when invoke then saves record without route and emits Success`() = runTest {
        val contract = createContract()
        every { rentingRepository.getContract() } returns flowOf(contract)

        val input = AddOdometerRecordUseCase.Input(
            odometerValue = 120.5,
            timestamp = 3000L,
            label = "Morning commute",
            fuelAmount = 25.0,
            encodedPolyline = null
        )

        val emissions = useCase(input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is AddOdometerRecordUseCase.Output.Progress)
        assertTrue(emissions[1] is AddOdometerRecordUseCase.Output.Success)

        coVerify(exactly = 1) {
            historyRepository.saveRecord(
                match<OdometerRecord> {
                    it.contractId == contract.id &&
                            it.odometerValue == 120.5 &&
                            it.timestamp == 3000L &&
                            it.label == "Morning commute" &&
                            it.fuelAmount == 25.0 &&
                            !it.hasRoute
                },
                isNull()
            )
        }
    }

    @Test
    fun `given contract and input with route when invoke then saves record with TripRoute and emits Success`() = runTest {
        val contract = createContract()
        every { rentingRepository.getContract() } returns flowOf(contract)

        val input = AddOdometerRecordUseCase.Input(
            odometerValue = 85.0,
            timestamp = 4000L,
            encodedPolyline = "u{~vFvyys@fG",
            pointCount = 42
        )

        val emissions = useCase(input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is AddOdometerRecordUseCase.Output.Progress)
        assertTrue(emissions[1] is AddOdometerRecordUseCase.Output.Success)

        coVerify(exactly = 1) {
            historyRepository.saveRecord(
                match<OdometerRecord> {
                    it.contractId == contract.id &&
                            it.odometerValue == 85.0 &&
                            it.hasRoute
                },
                match<TripRoute> {
                    it.encodedPolyline == "u{~vFvyys@fG" && it.pointCount == 42
                }
            )
        }
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        every { rentingRepository.getContract() } returns flow { throw RuntimeException("Database timeout") }

        val emissions = useCase(AddOdometerRecordUseCase.Input(odometerValue = 10.0, timestamp = 1000L)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is AddOdometerRecordUseCase.Output.Progress)
        val failure = emissions[1] as AddOdometerRecordUseCase.Output.Failure
        assertEquals("Database timeout", failure.message)
    }
}
