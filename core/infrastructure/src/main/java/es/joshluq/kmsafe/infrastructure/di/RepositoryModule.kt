package es.joshluq.kmsafe.infrastructure.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.kmsafe.infrastructure.repository.AuthRepositoryImpl
import es.joshluq.kmsafe.infrastructure.repository.DataManagementRepositoryImpl
import es.joshluq.kmsafe.infrastructure.repository.EntitlementsRepositoryImpl
import es.joshluq.kmsafe.infrastructure.repository.FuelExpenseRepositoryImpl
import es.joshluq.kmsafe.infrastructure.repository.HistoryRepositoryImpl
import es.joshluq.kmsafe.infrastructure.repository.MediaRepositoryImpl
import es.joshluq.kmsafe.infrastructure.repository.PreferencesRepositoryImpl
import es.joshluq.kmsafe.infrastructure.repository.RentingRepositoryImpl
import es.joshluq.kmsafe.infrastructure.repository.ServiceStationRepositoryImpl
import es.joshluq.kmsafe.infrastructure.repository.tracking.TrackingRepositoryImpl
import es.joshluq.kmsafe.domain.repository.AuthRepository
import es.joshluq.kmsafe.domain.repository.BluetoothRepository
import es.joshluq.kmsafe.domain.repository.DataManagementRepository
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.MediaRepository
import es.joshluq.kmsafe.domain.repository.PreferencesRepository
import es.joshluq.kmsafe.domain.repository.ReceiptRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import es.joshluq.kmsafe.infrastructure.repository.BluetoothRepositoryImpl
import es.joshluq.kmsafe.infrastructure.repository.ReceiptRepositoryImpl
import javax.inject.Singleton

/**
 * Dagger module for binding Repository interfaces to their implementations.
 * All implementations live in `:core:infrastructure`, keeping `:app` as a pure orchestration shell.
 */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds the [RentingRepositoryImpl] to the [RentingRepository] interface.
     */
    @Singleton
    @Binds
    abstract fun bindRentingRepository(repository: RentingRepositoryImpl): RentingRepository

    /**
     * Binds the [HistoryRepositoryImpl] to the [HistoryRepository] interface.
     */
    @Singleton
    @Binds
    abstract fun bindHistoryRepository(repository: HistoryRepositoryImpl): HistoryRepository

    /**
     * Binds the [PreferencesRepositoryImpl] to the [PreferencesRepository] interface.
     */
    @Singleton
    @Binds
    abstract fun bindPreferencesRepository(repository: PreferencesRepositoryImpl): PreferencesRepository

    /**
     * Binds the [DataManagementRepositoryImpl] to the [DataManagementRepository] interface.
     */
    @Singleton
    @Binds
    abstract fun bindDataManagementRepository(
        repository: DataManagementRepositoryImpl
    ): DataManagementRepository

    /**
     * Binds the [AuthRepositoryImpl] to the [AuthRepository] interface.
     */
    @Singleton
    @Binds
    abstract fun bindAuthRepository(
        repository: AuthRepositoryImpl
    ): AuthRepository

    /**
     * Binds the [TrackingRepositoryImpl] to the [TrackingRepository] interface.
     */
    @Singleton
    @Binds
    abstract fun bindTrackingRepository(
        repository: TrackingRepositoryImpl
    ): TrackingRepository

    /**
     * Binds the [EntitlementsRepositoryImpl] to the [EntitlementsRepository] interface.
     */
    @Singleton
    @Binds
    abstract fun bindEntitlementsRepository(
        repository: EntitlementsRepositoryImpl
    ): EntitlementsRepository

    /**
     * Binds the [MediaRepositoryImpl] to the [MediaRepository] interface.
     */
    @Singleton
    @Binds
    abstract fun bindMediaRepository(
        repository: MediaRepositoryImpl
    ): MediaRepository

    /**
     * Binds the [FuelExpenseRepositoryImpl] to the [FuelExpenseRepository] interface.
     */
    @Singleton
    @Binds
    abstract fun bindFuelExpenseRepository(
        repository: FuelExpenseRepositoryImpl
    ): FuelExpenseRepository

    /**
     * Binds the [ServiceStationRepositoryImpl] to the [ServiceStationRepository] interface.
     */
    @Singleton
    @Binds
    abstract fun bindServiceStationRepository(
        repository: ServiceStationRepositoryImpl
    ): ServiceStationRepository

    /**
     * Binds the [BluetoothRepositoryImpl] to the [BluetoothRepository] interface.
     */
    @Singleton
    @Binds
    abstract fun bindBluetoothRepository(
        repository: BluetoothRepositoryImpl
    ): BluetoothRepository

    /**
     * Binds the [ReceiptRepositoryImpl] to the [ReceiptRepository] interface.
     */
    @Singleton
    @Binds
    abstract fun bindReceiptRepository(
        repository: ReceiptRepositoryImpl
    ): ReceiptRepository

    @Singleton
    @Binds
    abstract fun bindAppOverlayRepository(
        repository: es.joshluq.kmsafe.infrastructure.repository.AppOverlayRepositoryImpl
    ): es.joshluq.kmsafe.domain.repository.AppOverlayRepository
}
