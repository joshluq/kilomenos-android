package es.joshluq.kmsafe.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.kmsafe.data.location.TrackingRepositoryImpl
import es.joshluq.kmsafe.data.repository.AuthRepositoryImpl
import es.joshluq.kmsafe.data.repository.DataManagementRepositoryImpl
import es.joshluq.kmsafe.data.repository.EntitlementsRepositoryImpl
import es.joshluq.kmsafe.data.repository.HistoryRepositoryImpl
import es.joshluq.kmsafe.data.repository.MediaRepositoryImpl
import es.joshluq.kmsafe.data.repository.PreferencesRepositoryImpl
import es.joshluq.kmsafe.data.repository.RentingRepositoryImpl
import es.joshluq.kmsafe.domain.repository.AuthRepository
import es.joshluq.kmsafe.domain.repository.DataManagementRepository
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.MediaRepository
import es.joshluq.kmsafe.domain.repository.PreferencesRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import javax.inject.Singleton

/**
 * Dagger module for providing Repository dependencies.
 */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Singleton
    @Binds
    abstract fun bindRentingRepository(repository: RentingRepositoryImpl): RentingRepository

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
