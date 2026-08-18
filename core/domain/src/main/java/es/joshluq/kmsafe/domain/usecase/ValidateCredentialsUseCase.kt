package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.di.EmailValidator
import es.joshluq.kmsafe.domain.di.PasswordValidator
import es.joshluq.kmsafe.domain.validator.Validator
import javax.inject.Inject

/**
 * Use case to validate login credentials asynchronously following the UseCase pattern.
 */
class ValidateCredentialsUseCase @Inject constructor(
    @EmailValidator private val emailValidator: @JvmSuppressWildcards Validator<String>,
    @PasswordValidator private val passwordValidator: @JvmSuppressWildcards  Validator<String>
) : UseCase<ValidateCredentialsUseCase.Input, ValidateCredentialsUseCase.Output> {

    override suspend fun invoke(input: Input): Result<Output> {
        val isEmailValid = emailValidator.isValid(input.email)
        val isPasswordValid = passwordValidator.isValid(input.password)

        return Result.success(
            Output(
                isEmailValid = isEmailValid,
                isPasswordValid = isPasswordValid,
                canLogin = isEmailValid && isPasswordValid
            )
        )
    }

    data class Input(
        val email: String,
        val password: String
    ) : UseCaseInput

    data class Output(
        val isEmailValid: Boolean,
        val isPasswordValid: Boolean,
        val canLogin: Boolean
    ) : UseCaseOutput
}
