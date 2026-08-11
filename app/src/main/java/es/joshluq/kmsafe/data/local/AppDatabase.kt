package es.joshluq.kmsafe.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import es.joshluq.kmsafe.data.local.dao.OdometerRecordDao
import es.joshluq.kmsafe.data.local.dao.RentingContractDao
import es.joshluq.kmsafe.data.local.dao.TripRouteDao
import es.joshluq.kmsafe.data.local.entity.OdometerRecordEntity
import es.joshluq.kmsafe.data.local.entity.RentingContractEntity
import es.joshluq.kmsafe.data.local.entity.TripRouteEntity

/**
 * Main Room database for the KiloMenos application.
 */
@Database(
    entities = [
        RentingContractEntity::class,
        OdometerRecordEntity::class,
        TripRouteEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rentingContractDao(): RentingContractDao
    abstract fun odometerRecordDao(): OdometerRecordDao
    abstract fun tripRouteDao(): TripRouteDao

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
    }
}
