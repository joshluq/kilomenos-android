package es.joshluq.kmsafe.infrastructure.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import es.joshluq.kmsafe.infrastructure.InfrastructureConfig
import es.joshluq.kmsafe.infrastructure.local.AppDatabase
import es.joshluq.kmsafe.infrastructure.local.dao.FuelExpenseDao
import es.joshluq.kmsafe.infrastructure.local.dao.OdometerRecordDao
import es.joshluq.kmsafe.infrastructure.local.dao.RentingContractDao
import es.joshluq.kmsafe.infrastructure.local.dao.ServiceStationDao
import es.joshluq.kmsafe.infrastructure.local.dao.TripRouteDao
import javax.inject.Singleton

/**
 * Hilt module for providing data-related dependencies, including Room database and DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    /**
     * Provides the singleton instance of [AppDatabase].
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        config: InfrastructureConfig
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            config.databaseName
        ).addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17
            ).build()
        }

    /**
     * Provides the [RentingContractDao] from the [AppDatabase].
     */
    @Provides
    @Singleton
    fun provideRentingContractDao(database: AppDatabase): RentingContractDao {
        return database.rentingContractDao()
    }

    /**
     * Provides the [OdometerRecordDao] from the [AppDatabase].
     */
    @Provides
    @Singleton
    fun provideOdometerRecordDao(database: AppDatabase): OdometerRecordDao {
        return database.odometerRecordDao()
    }

    /**
     * Provides the [TripRouteDao] from the [AppDatabase].
     */
    @Provides
    @Singleton
    fun provideTripRouteDao(database: AppDatabase): TripRouteDao {
        return database.tripRouteDao()
    }

    /**
     * Provides the [FuelExpenseDao] from the [AppDatabase].
     */
    @Provides
    @Singleton
    fun provideFuelExpenseDao(database: AppDatabase): FuelExpenseDao {
        return database.fuelExpenseDao()
    }

    /**
     * Provides the [ServiceStationDao] from the [AppDatabase].
     */
    @Provides
    @Singleton
    fun provideServiceStationDao(database: AppDatabase): ServiceStationDao {
        return database.serviceStationDao()
    }
}
