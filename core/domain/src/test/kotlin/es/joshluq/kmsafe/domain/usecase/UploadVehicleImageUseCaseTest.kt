package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
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

class UploadVehicleImageUseCaseTest {

    private val repository: RentingRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: UploadVehicleImageUseCase

    @Before
    fun setUp() {
        useCase = UploadVehicleImageUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given image bytes and fileName when invoke then uploads image and emits Progress then Success`() = runTest {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val fileName = "vehicle_front.jpg"
        val expectedUrl = "https://storage.example.com/vehicle_front.jpg"

        every { repository.uploadVehicleImage(bytes, fileName) } returns flowOf(expectedUrl)

        val emissions = useCase(UploadVehicleImageUseCase.Input(bytes, fileName)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is UploadVehicleImageUseCase.Output.Progress)
        val success = emissions[1] as UploadVehicleImageUseCase.Output.Success
        assertEquals(expectedUrl, success.imageUrl)

        coVerify(exactly = 1) { repository.uploadVehicleImage(bytes, fileName) }
    }

    @Test
    fun `given upload exception when invoke then catches and emits Failure`() = runTest {
        val bytes = byteArrayOf(1, 2, 3)
        val fileName = "vehicle.png"

        every { repository.uploadVehicleImage(bytes, fileName) } returns flow { throw RuntimeException("Network timeout") }

        val emissions = useCase(UploadVehicleImageUseCase.Input(bytes, fileName)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is UploadVehicleImageUseCase.Output.Progress)
        assertTrue(emissions[1] is UploadVehicleImageUseCase.Output.Failure)
    }
}
