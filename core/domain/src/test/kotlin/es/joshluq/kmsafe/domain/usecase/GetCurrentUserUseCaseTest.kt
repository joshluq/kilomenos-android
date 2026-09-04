package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.repository.AuthRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetCurrentUserUseCaseTest {

    private val repository: AuthRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: GetCurrentUserUseCase

    @Before
    fun setUp() {
        useCase = GetCurrentUserUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given logged in user when invoke then emits Success with user`() = runTest {
        val user = User(id = "u1", email = "user@test.com", name = "Test User")
        every { repository.getCurrentUser() } returns flowOf(user)

        val emissions = useCase(GetCurrentUserUseCase.Input).toList()

        assertEquals(1, emissions.size)
        val success = emissions[0] as GetCurrentUserUseCase.Output.Success
        assertEquals(user, success.user)
    }

    @Test
    fun `given no logged in user when invoke then emits Success with null user`() = runTest {
        every { repository.getCurrentUser() } returns flowOf(null)

        val emissions = useCase(GetCurrentUserUseCase.Input).toList()

        assertEquals(1, emissions.size)
        val success = emissions[0] as GetCurrentUserUseCase.Output.Success
        assertNull(success.user)
    }
}
