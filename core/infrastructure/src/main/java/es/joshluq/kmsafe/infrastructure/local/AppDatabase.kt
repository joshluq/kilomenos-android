package es.joshluq.kmsafe.infrastructure.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import es.joshluq.kmsafe.infrastructure.local.dao.FuelExpenseDao
import es.joshluq.kmsafe.infrastructure.local.dao.OdometerRecordDao
import es.joshluq.kmsafe.infrastructure.local.dao.RentingContractDao
import es.joshluq.kmsafe.infrastructure.local.dao.ServiceStationDao
import es.joshluq.kmsafe.infrastructure.local.dao.TripRouteDao
import es.joshluq.kmsafe.infrastructure.local.entity.FuelExpenseEntity
import es.joshluq.kmsafe.infrastructure.local.entity.OdometerRecordEntity
import es.joshluq.kmsafe.infrastructure.local.entity.RentingContractEntity
import es.joshluq.kmsafe.infrastructure.local.entity.ServiceStationEntity
import es.joshluq.kmsafe.infrastructure.local.entity.TripRouteEntity

/**
 * Main Room database for the KiloMenos application.
 */
@Database(
    entities = [
        RentingContractEntity::class,
        OdometerRecordEntity::class,
        TripRouteEntity::class,
        FuelExpenseEntity::class,
        ServiceStationEntity::class
    ],
    version = 17,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rentingContractDao(): RentingContractDao
    abstract fun odometerRecordDao(): OdometerRecordDao
    abstract fun tripRouteDao(): TripRouteDao
    abstract fun fuelExpenseDao(): FuelExpenseDao
    abstract fun serviceStationDao(): ServiceStationDao

    companion object {
        /**
         * Migration from version 1 to 2:
         * - Recreates 'renting_contract' table to support AUTOINCREMENT and 'isSelected' column.
         * - Migrates existing contract (id 0) to id 1 and marks it as selected.
         * - Updates 'odometer_record' table to add 'contractId' and links records to id 1.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS renting_contract_new")
                db.execSQL(
                    """
                    CREATE TABLE renting_contract_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        vehicleName TEXT NOT NULL,
                        startDate INTEGER NOT NULL,
                        durationMonths INTEGER NOT NULL,
                        totalKms INTEGER NOT NULL,
                        startOdometer INTEGER NOT NULL,
                        currentOdometer INTEGER NOT NULL,
                        isSelected INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO renting_contract_new (id, vehicleName, startDate, durationMonths, totalKms, startOdometer, currentOdometer, isSelected)
                    SELECT id + 1, vehicleName, startDate, durationMonths, totalKms, startOdometer, currentOdometer, 1
                    FROM renting_contract
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE renting_contract")
                db.execSQL("ALTER TABLE renting_contract_new RENAME TO renting_contract")

                // Add contractId to odometer_record
                db.execSQL("ALTER TABLE odometer_record ADD COLUMN contractId INTEGER NOT NULL DEFAULT 1")
            }
        }

        /**
         * Migration from version 2 to 3:
         * - Detects if the database is in a corrupted v2 state (missing 'isSelected' or 'contractId').
         * - Performs schema repair if necessary.
         * - Ensures at least one contract is marked as selected.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Check if 'isSelected' exists in renting_contract
                val cursor = db.query("PRAGMA table_info(renting_contract)")
                var hasIsSelected = false
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex != -1) {
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameIndex)
                        if (name == "isSelected") {
                            hasIsSelected = true
                            break
                        }
                    }
                }
                cursor.close()

                if (!hasIsSelected) {
                    // Repair Path
                    db.execSQL("DROP TABLE IF EXISTS renting_contract_new")
                    db.execSQL(
                        """
                        CREATE TABLE renting_contract_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            vehicleName TEXT NOT NULL,
                            startDate INTEGER NOT NULL,
                            durationMonths INTEGER NOT NULL,
                            totalKms INTEGER NOT NULL,
                            startOdometer INTEGER NOT NULL,
                            currentOdometer INTEGER NOT NULL,
                            isSelected INTEGER NOT NULL DEFAULT 1
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        INSERT INTO renting_contract_new (id, vehicleName, startDate, durationMonths, totalKms, startOdometer, currentOdometer, isSelected)
                        SELECT id + 1, vehicleName, startDate, durationMonths, totalKms, startOdometer, currentOdometer, 1
                        FROM renting_contract
                        """.trimIndent()
                    )

                    db.execSQL("DROP TABLE renting_contract")
                    db.execSQL("ALTER TABLE renting_contract_new RENAME TO renting_contract")

                    val recordsCursor = db.query("PRAGMA table_info(odometer_record)")
                    var hasContractId = false
                    val recNameIndex = recordsCursor.getColumnIndex("name")
                    if (recNameIndex != -1) {
                        while (recordsCursor.moveToNext()) {
                            val name = recordsCursor.getString(recNameIndex)
                            if (name == "contractId") {
                                hasContractId = true
                                break
                            }
                        }
                    }
                    recordsCursor.close()

                    if (!hasContractId) {
                        db.execSQL("ALTER TABLE odometer_record ADD COLUMN contractId INTEGER NOT NULL DEFAULT 1")
                    } else {
                        db.execSQL("UPDATE odometer_record SET contractId = 1 WHERE contractId = 0")
                    }
                } else {
                    db.execSQL(
                        """
                        UPDATE renting_contract 
                        SET isSelected = 1 
                        WHERE id = (SELECT id FROM renting_contract LIMIT 1)
                        AND NOT EXISTS (SELECT 1 FROM renting_contract WHERE isSelected = 1)
                        """.trimIndent()
                    )
                }
            }
        }

        /**
         * Migration from version 3 to 4:
         * - Add 'label' and 'fuelAmount' columns to 'odometer_record' table.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE odometer_record ADD COLUMN label TEXT")
                db.execSQL("ALTER TABLE odometer_record ADD COLUMN fuelAmount REAL")
            }
        }

        /**
         * Migration from version 4 to 5:
         * - Add 'vehicleImageUrl' column to 'renting_contract' table.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE renting_contract ADD COLUMN vehicleImageUrl TEXT")
            }
        }

        /**
         * Migration from version 5 to 6:
         * - Add 'userId' column to 'renting_contract' table.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE renting_contract ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Migration from version 6 to 7:
         * - Add 'syncStatus' column to 'renting_contract' and 'odometer_record' tables.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE renting_contract ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE odometer_record ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            }
        }

        /**
         * Migration from version 7 to 8:
         * - Recreates 'renting_contract' and 'odometer_record' tables to ensure all IDs are TEXT.
         * - Eliminates legacy AUTOINCREMENT behaviors.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Recreate renting_contract
                db.execSQL("DROP TABLE IF EXISTS renting_contract_new")
                db.execSQL(
                    """
                    CREATE TABLE renting_contract_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        userId TEXT NOT NULL DEFAULT '',
                        vehicleName TEXT NOT NULL,
                        startDate INTEGER NOT NULL,
                        durationMonths INTEGER NOT NULL,
                        totalKms INTEGER NOT NULL,
                        startOdometer INTEGER NOT NULL,
                        currentOdometer INTEGER NOT NULL,
                        isSelected INTEGER NOT NULL DEFAULT 0,
                        vehicleImageUrl TEXT,
                        syncStatus TEXT NOT NULL DEFAULT 'SYNCED'
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO renting_contract_new (id, userId, vehicleName, startDate, durationMonths, totalKms, startOdometer, currentOdometer, isSelected, vehicleImageUrl, syncStatus)
                    SELECT CAST(id AS TEXT), userId, vehicleName, startDate, durationMonths, totalKms, startOdometer, currentOdometer, isSelected, vehicleImageUrl, syncStatus
                    FROM renting_contract
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE renting_contract")
                db.execSQL("ALTER TABLE renting_contract_new RENAME TO renting_contract")

                // 2. Recreate odometer_record
                db.execSQL("DROP TABLE IF EXISTS odometer_record_new")
                db.execSQL(
                    """
                    CREATE TABLE odometer_record_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        contractId TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        odometerValue INTEGER NOT NULL,
                        isInitialRecord INTEGER NOT NULL,
                        label TEXT,
                        fuelAmount REAL,
                        syncStatus TEXT NOT NULL DEFAULT 'SYNCED'
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO odometer_record_new (id, contractId, timestamp, odometerValue, isInitialRecord, label, fuelAmount, syncStatus)
                    SELECT CAST(id AS TEXT), CAST(contractId AS TEXT), timestamp, odometerValue, isInitialRecord, label, fuelAmount, syncStatus
                    FROM odometer_record
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE odometer_record")
                db.execSQL("ALTER TABLE odometer_record_new RENAME TO odometer_record")
            }
        }

        /**
         * Migration from version 8 to 9:
         * - Add 'bluetoothDeviceAddress' column to 'renting_contract' table.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE renting_contract ADD COLUMN bluetoothDeviceAddress TEXT")
            }
        }

        /**
         * Migration from version 9 to 10:
         * - Add 'hasRoute' column to 'odometer_record' table.
         * - Create 'trip_route' table for storing encoded polylines.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add column to odometer_record
                db.execSQL("ALTER TABLE odometer_record ADD COLUMN hasRoute INTEGER NOT NULL DEFAULT 0")

                // 2. Create trip_route table
                db.execSQL(
                    """
                    CREATE TABLE trip_route (
                        recordId TEXT PRIMARY KEY NOT NULL,
                        encodedPolyline TEXT NOT NULL,
                        pointCount INTEGER NOT NULL,
                        FOREIGN KEY(recordId) REFERENCES odometer_record(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migration from version 10 to 11:
         * - Add 'excessDistancePrice' column to 'renting_contract' table.
         * - Add 'courtesyMarginKms' column to 'renting_contract' table.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE renting_contract ADD COLUMN excessDistancePrice REAL")
                db.execSQL("ALTER TABLE renting_contract ADD COLUMN courtesyMarginKms INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Migration from version 11 to 12:
         * - Add 'bluetoothDeviceName' column to 'renting_contract' table.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE renting_contract ADD COLUMN bluetoothDeviceName TEXT")
            }
        }

        /**
         * Migration from version 12 to 13:
         * - Create 'service_stations' table and indices.
         * - Create 'fuel_expenses' table with foreign key to 'renting_contract' and indices.
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS service_stations (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        brand TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        address TEXT NOT NULL,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        availableEnergies TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_stations_isFavorite ON service_stations(isFavorite)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_stations_latitude_longitude ON service_stations(latitude, longitude)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS fuel_expenses (
                        id TEXT PRIMARY KEY NOT NULL,
                        vehicleId TEXT NOT NULL,
                        stationId TEXT,
                        stationName TEXT,
                        timestamp INTEGER NOT NULL,
                        fuelType TEXT NOT NULL,
                        unitPrice REAL NOT NULL,
                        volumeQuantity REAL NOT NULL,
                        totalCost REAL NOT NULL,
                        odometerAtExpense INTEGER,
                        isFullTank INTEGER NOT NULL DEFAULT 1,
                        notes TEXT,
                        syncStatus TEXT NOT NULL DEFAULT 'SYNCED',
                        FOREIGN KEY(vehicleId) REFERENCES renting_contract(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fuel_expenses_vehicleId ON fuel_expenses(vehicleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fuel_expenses_stationId ON fuel_expenses(stationId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fuel_expenses_timestamp ON fuel_expenses(timestamp)")
            }
        }
        /**
         * Migration from version 13 to 14:
         * - Adds 'kmSinceLastRefuel' and 'consumptionPer100km' columns to 'fuel_expenses' table
         *   to support the hybrid A+C consumption tracking algorithm.
         */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE fuel_expenses ADD COLUMN kmSinceLastRefuel INTEGER")
                db.execSQL("ALTER TABLE fuel_expenses ADD COLUMN consumptionPer100km REAL")
            }
        }

        /**
         * Migration from version 14 to 15:
         * - Adds 'fuelType' column to 'renting_contract' table.
         */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE renting_contract ADD COLUMN fuelType TEXT NOT NULL DEFAULT 'GASOLINE_95'")
            }
        }

        /**
         * Migration from version 15 to 16:
         * - Add metadata (createdAt, updatedAt, syncStatus) to 'service_stations'.
         * - Add 'updatedAt' to 'fuel_expenses' for Last-Write-Wins sync logic.
         */
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                // Update service_stations
                db.execSQL("ALTER TABLE service_stations ADD COLUMN createdAt INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE service_stations ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE service_stations ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")

                // Update fuel_expenses
                db.execSQL("ALTER TABLE fuel_expenses ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $now")
            }
        }

        /**
         * Migration from version 16 to 17:
         * - Migrates integer mileage/odometer columns to REAL to support decimal precision (API V2).
         * - Affected tables: renting_contract, odometer_record, fuel_expenses.
         */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Migrate renting_contract
                db.execSQL("DROP TABLE IF EXISTS renting_contract_new")
                db.execSQL(
                    """
                    CREATE TABLE renting_contract_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        userId TEXT NOT NULL,
                        vehicleName TEXT NOT NULL,
                        startDate INTEGER NOT NULL,
                        durationMonths INTEGER NOT NULL,
                        totalKms REAL NOT NULL,
                        startOdometer REAL NOT NULL,
                        currentOdometer REAL NOT NULL,
                        isSelected INTEGER NOT NULL,
                        vehicleImageUrl TEXT,
                        bluetoothDeviceName TEXT,
                        bluetoothDeviceAddress TEXT,
                        excessDistancePrice REAL,
                        courtesyMarginKms REAL NOT NULL DEFAULT 0.0,
                        fuelType TEXT NOT NULL DEFAULT 'GASOLINE_95',
                        syncStatus TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO renting_contract_new (id, userId, vehicleName, startDate, durationMonths, totalKms, startOdometer, currentOdometer, isSelected, vehicleImageUrl, bluetoothDeviceName, bluetoothDeviceAddress, excessDistancePrice, courtesyMarginKms, fuelType, syncStatus)
                    SELECT id, userId, vehicleName, startDate, durationMonths, CAST(totalKms AS REAL), CAST(startOdometer AS REAL), CAST(currentOdometer AS REAL), isSelected, vehicleImageUrl, bluetoothDeviceName, bluetoothDeviceAddress, excessDistancePrice, CAST(courtesyMarginKms AS REAL), fuelType, syncStatus
                    FROM renting_contract
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE renting_contract")
                db.execSQL("ALTER TABLE renting_contract_new RENAME TO renting_contract")

                // 2. Migrate odometer_record
                db.execSQL("DROP TABLE IF EXISTS odometer_record_new")
                db.execSQL(
                    """
                    CREATE TABLE odometer_record_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        contractId TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        odometerValue REAL NOT NULL,
                        isInitialRecord INTEGER NOT NULL,
                        label TEXT,
                        fuelAmount REAL,
                        hasRoute INTEGER NOT NULL DEFAULT 0,
                        syncStatus TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO odometer_record_new (id, contractId, timestamp, odometerValue, isInitialRecord, label, fuelAmount, hasRoute, syncStatus)
                    SELECT id, contractId, timestamp, CAST(odometerValue AS REAL), isInitialRecord, label, fuelAmount, hasRoute, syncStatus
                    FROM odometer_record
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE odometer_record")
                db.execSQL("ALTER TABLE odometer_record_new RENAME TO odometer_record")

                // 3. Migrate fuel_expenses
                db.execSQL("DROP TABLE IF EXISTS fuel_expenses_new")
                db.execSQL(
                    """
                    CREATE TABLE fuel_expenses_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        vehicleId TEXT NOT NULL,
                        stationId TEXT,
                        stationName TEXT,
                        timestamp INTEGER NOT NULL,
                        fuelType TEXT NOT NULL,
                        unitPrice REAL NOT NULL,
                        volumeQuantity REAL NOT NULL,
                        totalCost REAL NOT NULL,
                        odometerAtExpense REAL,
                        isFullTank INTEGER NOT NULL DEFAULT 1,
                        notes TEXT,
                        kmSinceLastRefuel REAL,
                        consumptionPer100km REAL,
                        updatedAt INTEGER NOT NULL,
                        syncStatus TEXT NOT NULL,
                        FOREIGN KEY(vehicleId) REFERENCES renting_contract(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO fuel_expenses_new (id, vehicleId, stationId, stationName, timestamp, fuelType, unitPrice, volumeQuantity, totalCost, odometerAtExpense, isFullTank, notes, kmSinceLastRefuel, consumptionPer100km, updatedAt, syncStatus)
                    SELECT id, vehicleId, stationId, stationName, timestamp, fuelType, unitPrice, volumeQuantity, totalCost, CAST(odometerAtExpense AS REAL), isFullTank, notes, CAST(kmSinceLastRefuel AS REAL), consumptionPer100km, updatedAt, syncStatus
                    FROM fuel_expenses
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE fuel_expenses")
                db.execSQL("ALTER TABLE fuel_expenses_new RENAME TO fuel_expenses")
                
                // Re-create indices for fuel_expenses
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fuel_expenses_vehicleId ON fuel_expenses(vehicleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fuel_expenses_stationId ON fuel_expenses(stationId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fuel_expenses_timestamp ON fuel_expenses(timestamp)")
            }
        }
    }
}
