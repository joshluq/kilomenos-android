package es.joshluq.kmsafe.infrastructure.repository

import es.joshluq.foundationkit.coroutines.DispatcherProvider
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.infrastructure.mapper.ErrorMapper
import es.joshluq.kmsafe.infrastructure.remote.api.ReceiptsApiService
import es.joshluq.kmsafe.infrastructure.remote.api.StorageApiService
import es.joshluq.kmsafe.infrastructure.remote.dto.ArithmeticCheckDto
import es.joshluq.kmsafe.infrastructure.remote.dto.ExtractedReceiptDataDto
import es.joshluq.kmsafe.infrastructure.remote.dto.ProcessReceiptRequest
import es.joshluq.kmsafe.infrastructure.remote.dto.ProcessReceiptResponse
import es.joshluq.kmsafe.infrastructure.remote.dto.RemoteFuelType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val storageApiService: StorageApiService = mockk()
    private val receiptsApiService: ReceiptsApiService = mockk()
    private val errorMapper: ErrorMapper = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)
    private val dispatchers: DispatcherProvider = mockk()

    private lateinit var repository: ReceiptRepositoryImpl

    @Before
    fun setup() {
        every { dispatchers.io } returns testDispatcher
        repository = ReceiptRepositoryImpl(
            storageApiService = storageApiService,
            receiptsApiService = receiptsApiService,
            errorMapper = errorMapper,
            logger = logger,
            dispatchers = dispatchers
        )
    }

    @Test
    fun `uploadReceiptImage returns relative path when upload succeeds`() = runTest(testDispatcher) {
        val userId = "user-123"
        val fileName = "receipt-1.jpg"
        val bytes = byteArrayOf(1, 2, 3)

        coEvery {
            storageApiService.uploadReceipt(userId, fileName, any(), "true")
        } returns Response.success(Unit)

        val result = repository.uploadReceiptImage(userId, fileName, bytes).first()

        assertEquals("user-123/receipt-1.jpg", result)
        coVerify(exactly = 1) { storageApiService.uploadReceipt(userId, fileName, any(), "true") }
    }

    @Test(expected = KmException::class)
    fun `uploadReceiptImage throws KmException when server returns error`() = runTest(testDispatcher) {
        val userId = "user-123"
        val fileName = "receipt-1.jpg"
        val bytes = byteArrayOf(1, 2, 3)

        coEvery {
            storageApiService.uploadReceipt(userId, fileName, any(), "true")
        } returns Response.error(500, "Server error".toResponseBody())

        repository.uploadReceiptImage(userId, fileName, bytes).first()
    }

    @Test
    fun `deleteReceiptImage calls storage delete successfully`() = runTest(testDispatcher) {
        val path = "user-123/receipt-1.jpg"

        coEvery {
            storageApiService.deleteReceipt("user-123", "receipt-1.jpg")
        } returns Response.success(Unit)

        val result = repository.deleteReceiptImage(path).first()

        assertEquals(Unit, result)
        coVerify(exactly = 1) { storageApiService.deleteReceipt("user-123", "receipt-1.jpg") }
    }

    @Test
    fun `processReceipt returns domain ReceiptScanResult when response is successful`() = runTest(testDispatcher) {
        val filePath = "user-123/receipt-1.jpg"
        val responseDto = ProcessReceiptResponse(
            success = true,
            data = ExtractedReceiptDataDto(
                stationName = "Repsol M-40",
                purchaseDate = "2026-08-25T14:30:00Z",
                fuelType = RemoteFuelType.DIESEL,
                liters = 45.0,
                pricePerLiter = 1.50,
                totalAmount = 67.50,
                confidenceScore = 0.95,
                isFuelReceipt = true,
                arithmeticCheck = ArithmeticCheckDto(valid = true, calculatedAmount = 67.50, discrepancy = 0.0)
            )
        )

        coEvery {
            receiptsApiService.processReceipt(ProcessReceiptRequest(filePath))
        } returns Response.success(responseDto)

        val result = repository.processReceipt(filePath).first()

        assertNotNull(result)
        assertEquals("Repsol M-40", result.stationName)
        assertEquals(FuelType.DIESEL, result.fuelType)
        assertEquals(45.0, result.liters, 0.001)
        assertEquals(1.50, result.pricePerLiter, 0.001)
        assertEquals(67.50, result.totalAmount, 0.001)
        assertTrue(result.arithmeticCheck.valid)
        assertEquals(filePath, result.storageFilePath)
    }
}
