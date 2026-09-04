package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.repository.RentingRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CheckDatabaseOwnerUseCaseTest {

    private val repository: RentingRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: CheckDatabaseOwnerUseCase

    @Before
    fun setUp() {
        useCase = CheckDatabaseOwnerUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given ownerId when invoke then emits Progress and Success with ownerId`() = runTest {
        coEvery { repository.getDatabaseOwnerId() } returns "user-123"

        val emissions = useCase(CheckDatabaseOwnerUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is CheckDatabaseOwnerUseCase.Output.Progress)
        val success = emissions[1] as CheckDatabaseOwnerUseCase.Output.Success
        assertEquals("user-123", success.ownerId)
    }

    @Test
    fun `given null ownerId when invoke then emits Progress and Success with null`() = runTest {
        coEvery { repository.getDatabaseOwnerId() } returns null

        val emissions = useCase(CheckDatabaseOwnerUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is CheckDatabaseOwnerUseCase.Output.Progress)
        val success = emissions[1] as CheckDatabaseOwnerUseCase.Output.Success
        assertNull(success.ownerId)
    }
}
