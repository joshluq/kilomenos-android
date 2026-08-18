package es.joshluq.kmsafe.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.kmsafe.domain.di.EmailValidator
import es.joshluq.kmsafe.domain.di.PasswordValidator
import es.joshluq.kmsafe.domain.validator.EmailValidator as EmailValidatorImpl
import es.joshluq.kmsafe.domain.validator.PasswordValidator as PasswordValidatorImpl
import es.joshluq.kmsafe.domain.validator.Validator

/**
 * Dagger module for providing Validator dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ValidatorModule {

    @Binds
    @EmailValidator
    abstract fun bindEmailValidator(
        validator: EmailValidatorImpl
    ): Validator<String>

    @Binds
    @PasswordValidator
    abstract fun bindPasswordValidator(
        validator: PasswordValidatorImpl
    ): Validator<String>
}
