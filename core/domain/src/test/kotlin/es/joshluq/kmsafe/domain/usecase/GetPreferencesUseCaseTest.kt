package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.model.UserPreferences
import es.joshluq.kmsafe.domain.repository.AuthRepository
import es.joshluq.kmsafe.domain.repository.PreferencesRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetPreferencesUseCaseTest {

    private val repository: PreferencesRepository = mockk()
    private val authRepository: AuthRepository = mockk()

    private lateinit var useCase: GetPreferencesUseCase

    @Before
    fun setUp() {
        useCase = GetPreferencesUseCaseImpl(repository, authRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given logged in user when invoke then returns user preferences`() = runTest {
        val user = User(id = "u1", email = "test@example.com", name = "User")
        val userPrefs = UserPreferences(rememberEmail = true, lastEmail = "test@example.com")

        every { authRepository.getCurrentUser() } returns flowOf(user)
        every { repository.getPreferences("u1") } returns flowOf(userPrefs)

        val emissions = useCase(GetPreferencesUseCase.Input).toList()

        assertEquals(1, emissions.size)
        val success = emissions[0] as GetPreferencesUseCase.Output.Success
        assertEquals(userPrefs, success.preferences)
    }

    @Test
    fun `given no logged in user when invoke then returns global preferences`() = runTest {
        val globalPrefs = UserPreferences(rememberEmail = false, lastEmail = "")

        every { authRepository.getCurrentUser() } returns flowOf(null)
        every { repository.getGlobalPreferences() } returns flowOf(globalPrefs)

        val emissions = useCase(GetPreferencesUseCase.Input).toList()

        assertEquals(1, emissions.size)
        val success = emissions[0] as GetPreferencesUseCase.Output.Success
        assertEquals(globalPrefs, success.preferences)
    }
}
