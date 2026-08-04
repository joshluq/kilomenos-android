package es.joshluq.kmsafe.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import es.joshluq.kmsafe.BuildConfig
import es.joshluq.kmsafe.data.local.AppDatabase
import es.joshluq.kmsafe.data.local.dao.OdometerRecordDao
import es.joshluq.kmsafe.data.local.dao.RentingContractDao
import javax.inject.Singleton

/**
 * Hilt module for providing data-related dependencies, including Room database and repositories.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {



    companion object {

        private const val DATABASE_NAME = "kmsafe_db_${BuildConfig.FLAVOR}"

        /**
         * Provides the singleton instance of [AppDatabase].
         */
        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context
        ): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            ).addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8
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
    }
}
