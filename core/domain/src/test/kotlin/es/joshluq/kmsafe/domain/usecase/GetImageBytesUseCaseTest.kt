package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.repository.MediaRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetImageBytesUseCaseTest {

    private val repository: MediaRepository = mockk()

    private lateinit var useCase: GetImageBytesUseCase

    @Before
    fun setUp() {
        useCase = GetImageBytesUseCaseImpl(repository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given valid uri path when invoke and bytes returned then returns Success`() = runTest {
        val uriPath = "content://media/external/images/media/123"
        val expectedBytes = byteArrayOf(10, 20, 30)
        coEvery { repository.getFileBytes(uriPath) } returns expectedBytes

        val result = useCase(GetImageBytesUseCase.Input(uriPath))

        assertTrue(result.isSuccess)
        val output = result.getOrThrow() as GetImageBytesUseCase.Output.Success
        assertArrayEquals(expectedBytes, output.bytes)

        coVerify(exactly = 1) { repository.getFileBytes(uriPath) }
    }

    @Test
    fun `given uri path when invoke and bytes null then returns Failure`() = runTest {
        val uriPath = "content://invalid/path"
        coEvery { repository.getFileBytes(uriPath) } returns null

        val result = useCase(GetImageBytesUseCase.Input(uriPath))

        assertTrue(result.isFailure)
        coVerify(exactly = 1) { repository.getFileBytes(uriPath) }
    }
}
