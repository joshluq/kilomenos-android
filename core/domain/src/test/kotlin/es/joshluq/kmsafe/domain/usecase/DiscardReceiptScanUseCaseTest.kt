package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.repository.ReceiptRepository
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
import org.junit.Before
import org.junit.Test

class DiscardReceiptScanUseCaseTest {

    private val receiptRepository: ReceiptRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: DiscardReceiptScanUseCase

    @Before
    fun setUp() {
        useCase = DiscardReceiptScanUseCaseImpl(receiptRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given valid path when invoke then deletes image from storage and completes`() = runTest {
        val path = "user-1/receipt.jpg"
        every { receiptRepository.deleteReceiptImage(path) } returns flowOf(Unit)

        val emissions = useCase(DiscardReceiptScanUseCase.Input(path)).toList()

        assertEquals(1, emissions.size)
        assertEquals(DiscardReceiptScanUseCase.Output.Success, emissions[0])
        coVerify(exactly = 1) { receiptRepository.deleteReceiptImage(path) }
    }

    @Test
    fun `given error during deletion when invoke then catches and still completes gracefully`() = runTest {
        val path = "user-1/receipt.jpg"
        every { receiptRepository.deleteReceiptImage(path) } returns flow { throw RuntimeException("Network error") }

        val emissions = useCase(DiscardReceiptScanUseCase.Input(path)).toList()

        assertEquals(1, emissions.size)
        assertEquals(DiscardReceiptScanUseCase.Output.Success, emissions[0])
    }
}
