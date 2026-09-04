package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.di.EmailValidator
import es.joshluq.kmsafe.domain.di.PasswordValidator
import es.joshluq.kmsafe.domain.validator.Validator
import javax.inject.Inject

/**
 * Domain interface to validate login credentials asynchronously following the UseCase pattern.
 */
interface ValidateCredentialsUseCase : UseCase<ValidateCredentialsUseCase.Input, ValidateCredentialsUseCase.Output> {
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

class ValidateCredentialsUseCaseImpl @Inject constructor(
    @EmailValidator private val emailValidator: @JvmSuppressWildcards Validator<String>,
    @PasswordValidator private val passwordValidator: @JvmSuppressWildcards Validator<String>
) : ValidateCredentialsUseCase {

    override suspend fun invoke(input: ValidateCredentialsUseCase.Input): Result<ValidateCredentialsUseCase.Output> {
        val isEmailValid = emailValidator.isValid(input.email)
        val isPasswordValid = passwordValidator.isValid(input.password)

        return Result.success(
            ValidateCredentialsUseCase.Output(
                isEmailValid = isEmailValid,
                isPasswordValid = isPasswordValid,
                canLogin = isEmailValid && isPasswordValid
            )
        )
    }
}
