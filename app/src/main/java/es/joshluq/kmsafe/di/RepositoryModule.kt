package es.joshluq.kmsafe.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.kmsafe.domain.repository.*
import es.joshluq.kmsafe.data.repository.*
import es.joshluq.kmsafe.data.location.TrackingRepositoryImpl
import javax.inject.Singleton

/**
 * Dagger module for providing Repository dependencies.
 */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds the [RentingRepositoryImpl] to the [RentingRepository] interface.
     *
     * @param repository The implementation of the repository.
     * @return The bound repository.
     */
    @Singleton
    @Binds
    abstract fun bindRepository(repository: RentingRepositoryImpl): RentingRepository

    /**
     * Binds the [HistoryRepositoryImpl] to the [HistoryRepository] interface.
     */
    @Singleton
    @Binds
    abstract fun bindHistoryRepository(repository: HistoryRepositoryImpl): HistoryRepository

    @Singleton
    @Binds
    abstract fun bindPreferencesRepository(repository: PreferencesRepositoryImpl): PreferencesRepository

    @Singleton
    @Binds
    abstract fun bindDataManagementRepository(
        repository: DataManagementRepositoryImpl
    ): DataManagementRepository

    @Singleton
    @Binds
    abstract fun bindAuthRepository(
        repository: AuthRepositoryImpl
    ): AuthRepository

    @Singleton
    @Binds
    abstract fun bindTrackingRepository(
        repository: TrackingRepositoryImpl
    ): TrackingRepository

    @Singleton
    @Binds
    abstract fun bindEntitlementsRepository(
        repository: EntitlementsRepositoryImpl
    ): EntitlementsRepository

    @Singleton
    @Binds
    abstract fun bindMediaRepository(
        repository: MediaRepositoryImpl
    ): MediaRepository
}
