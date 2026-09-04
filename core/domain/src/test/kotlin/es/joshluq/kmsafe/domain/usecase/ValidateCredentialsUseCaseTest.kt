package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.validator.Validator
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidateCredentialsUseCaseTest {

    private val emailValidator: Validator<String> = mockk()
    private val passwordValidator: Validator<String> = mockk()

    private lateinit var useCase: ValidateCredentialsUseCase

    @Before
    fun setUp() {
        useCase = ValidateCredentialsUseCaseImpl(emailValidator, passwordValidator)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given valid email and valid password when invoke then returns canLogin true`() = runTest {
        every { emailValidator.isValid("test@example.com") } returns true
        every { passwordValidator.isValid("Password123!") } returns true

        val result = useCase(ValidateCredentialsUseCase.Input("test@example.com", "Password123!"))

        assertTrue(result.isSuccess)
        val output = result.getOrThrow()
        assertTrue(output.isEmailValid)
        assertTrue(output.isPasswordValid)
        assertTrue(output.canLogin)
    }

    @Test
    fun `given invalid email when invoke then returns canLogin false`() = runTest {
        every { emailValidator.isValid("invalid-email") } returns false
        every { passwordValidator.isValid("Password123!") } returns true

        val result = useCase(ValidateCredentialsUseCase.Input("invalid-email", "Password123!"))

        assertTrue(result.isSuccess)
        val output = result.getOrThrow()
        assertFalse(output.isEmailValid)
        assertTrue(output.isPasswordValid)
        assertFalse(output.canLogin)
    }

    @Test
    fun `given invalid password when invoke then returns canLogin false`() = runTest {
        every { emailValidator.isValid("test@example.com") } returns true
        every { passwordValidator.isValid("short") } returns false

        val result = useCase(ValidateCredentialsUseCase.Input("test@example.com", "short"))

        assertTrue(result.isSuccess)
        val output = result.getOrThrow()
        assertTrue(output.isEmailValid)
        assertFalse(output.isPasswordValid)
        assertFalse(output.canLogin)
    }
}
