package es.joshluq.kmsafe.domain.di

import javax.inject.Qualifier

/**
 * Qualifier for the Authenticated Retrofit instance.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Authenticated

/**
 * Qualifier for Email validation.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EmailValidator

/**
 * Qualifier for Password validation.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PasswordValidator
