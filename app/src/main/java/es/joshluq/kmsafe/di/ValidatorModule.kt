package es.joshluq.kmsafe.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.kmsafe.domain.validator.EmailValidator
import es.joshluq.kmsafe.domain.validator.PasswordValidator
import es.joshluq.kmsafe.domain.validator.Validator

/**
 * Dagger module for providing Validator dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ValidatorModule {

    @Binds
    @es.joshluq.kmsafe.di.EmailValidator
    abstract fun bindEmailValidator(
        validator: EmailValidator
    ): Validator<String>

    @Binds
    @es.joshluq.kmsafe.di.PasswordValidator
    abstract fun bindPasswordValidator(
        validator: PasswordValidator
    ): Validator<String>
}
