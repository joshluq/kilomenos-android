package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.repository.AuthRepository
import es.joshluq.kmsafe.domain.repository.PreferencesRepository
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdatePreferencesUseCaseTest {

    private val repository: PreferencesRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: UpdatePreferencesUseCase

    @Before
    fun setUp() {
        useCase = UpdatePreferencesUseCaseImpl(repository, authRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given logged in user when invoke then updates preferences and emits Success`() = runTest {
        val user = User(id = "u1", email = "test@example.com", name = "User")
        every { authRepository.getCurrentUser() } returns flowOf(user)

        val input = UpdatePreferencesUseCase.Input(
            rememberEmail = true,
            lastEmail = "test@example.com",
            showProjectionBanner = false,
            autoTrackingEnabled = true
        )

        val emissions = useCase(input).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0] is UpdatePreferencesUseCase.Output.Success)

        coVerify(exactly = 1) { repository.setRememberEmail(true) }
        coVerify(exactly = 1) { repository.saveLastEmail("test@example.com") }
        coVerify(exactly = 1) { repository.setShowProjectionBanner("u1", false) }
        coVerify(exactly = 1) { repository.setAutoTrackingEnabled("u1", true) }
    }

    @Test
    fun `given no logged in user when invoke then updates global preferences only and emits Success`() = runTest {
        every { authRepository.getCurrentUser() } returns flowOf(null)

        val input = UpdatePreferencesUseCase.Input(
            rememberEmail = false,
            lastEmail = "global@test.com"
        )

        val emissions = useCase(input).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0] is UpdatePreferencesUseCase.Output.Success)

        coVerify(exactly = 1) { repository.setRememberEmail(false) }
        coVerify(exactly = 1) { repository.saveLastEmail("global@test.com") }
        coVerify(exactly = 0) { repository.setAutoTrackingEnabled(any(), any()) }
    }
}
