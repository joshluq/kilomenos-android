package es.joshluq.kmsafe.infrastructure.repository.util

import androidx.room.withTransaction
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.infrastructure.local.AppDatabase
import es.joshluq.kmsafe.infrastructure.local.dao.FuelExpenseDao
import es.joshluq.kmsafe.infrastructure.local.dao.OdometerRecordDao
import es.joshluq.kmsafe.infrastructure.local.dao.RentingContractDao
import es.joshluq.kmsafe.infrastructure.local.dao.TripRouteDao
import es.joshluq.kmsafe.infrastructure.local.dao.ServiceStationDao
import es.joshluq.kmsafe.infrastructure.local.entity.toEntity
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.domain.model.SyncStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class to handle ID resolution and synchronization status updates
 * after remote operations.
 */
@Singleton
class SyncIdHandler @Inject constructor(
    private val appDatabase: AppDatabase,
    private val rentingDao: RentingContractDao,
    private val odometerDao: OdometerRecordDao,
    private val fuelExpenseDao: FuelExpenseDao,
    private val stationDao: ServiceStationDao,
    private val routeDao: TripRouteDao,
    private val logger: LoggerKit
) {

    /**
     * Resolves the identity of a RentingContract after a remote sync.
     * If the remote ID differs from the local one, it performs a swap.
     */
    suspend fun resolveRentingId(localContract: RentingContract, remoteContract: RentingContract) {
        if (localContract.id != remoteContract.id) {
            logger.w("SyncIdHandler", "ID Mismatch for Contract: ${localContract.id} -> ${remoteContract.id}")
            appDatabase.withTransaction {
                // 1. Persist the remote version with local UI state preserved
                val toPersist = remoteContract.copy(
                    isSelected = localContract.isSelected,
                    syncStatus = SyncStatus.SYNCED
                )
                rentingDao.insertContract(toPersist.toEntity())

                // 2. Migrate children before deleting the old parent to avoid CASCADE deletion
                odometerDao.updateContractId(oldId = localContract.id, newId = remoteContract.id)
                fuelExpenseDao.updateVehicleId(oldId = localContract.id, newId = remoteContract.id)

                // 3. Delete legacy local contract
                rentingDao.deleteContract(localContract.id)
            }
        } else {
            logger.d("SyncIdHandler", "ID Match for Contract: ${remoteContract.id}. Marking as SYNCED.")
            // Use partial update to avoid REPLACE and its CASCADE deletion trigger
            rentingDao.updateSyncStatus(remoteContract.id, SyncStatus.SYNCED.name)
        }
    }

    /**
     * Resolves the identity of an OdometerRecord after a remote sync.
     * Handles associated trip routes if an ID swap is required.
     */
    suspend fun resolveOdometerId(localRecord: OdometerRecord, remoteRecord: OdometerRecord) {
        // We preserve the hasRoute flag from local if it's true, because the remote creation 
        // response might not have it yet (route is uploaded in a separate step).
        val syncedRecord = remoteRecord.copy(
            syncStatus = SyncStatus.SYNCED,
            hasRoute = remoteRecord.hasRoute || localRecord.hasRoute
        )

        if (localRecord.id != remoteRecord.id) {
            logger.w("SyncIdHandler", "ID Mismatch for Record: ${localRecord.id} -> ${remoteRecord.id}")
            appDatabase.withTransaction {
                // Handle TripRoute migration if it exists
                val routeEntity = routeDao.getRouteByRecordIdSync(localRecord.id)
                routeEntity?.let {
                    routeDao.deleteRouteByRecordId(localRecord.id)
                    routeDao.insertRoute(it.copy(recordId = remoteRecord.id))
                }

                odometerDao.deleteRecord(localRecord.toEntity())
                odometerDao.insertRecord(syncedRecord.toEntity())
            }
        } else {
            logger.d("SyncIdHandler", "ID Match for Record: ${remoteRecord.id}. Marking as SYNCED.")
            odometerDao.insertRecord(syncedRecord.toEntity())
        }
    }

    /**
     * Resolves the identity of a ServiceStation after a remote sync.
     */
    suspend fun resolveStationId(localStation: ServiceStation, remoteStation: ServiceStation) {
        if (localStation.id != remoteStation.id) {
            logger.w("SyncIdHandler", "ID Mismatch for Station: ${localStation.id} -> ${remoteStation.id}")
            appDatabase.withTransaction {
                // 1. Persist the remote version
                stationDao.insertStation(remoteStation.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                
                // 2. Update all associated expenses to point to the new ID
                fuelExpenseDao.updateStationId(oldId = localStation.id, newId = remoteStation.id)
                
                // 3. Delete legacy local station
                stationDao.deleteStation(localStation.id)
            }
        } else {
            logger.d("SyncIdHandler", "ID Match for Station: ${remoteStation.id}. Marking as SYNCED.")
            stationDao.updateSyncStatus(remoteStation.id, SyncStatus.SYNCED.name)
        }
    }

    /**
     * Resolves the identity of a FuelExpense after a remote sync.
     */
    suspend fun resolveFuelExpenseId(localExpense: FuelExpense, remoteExpense: FuelExpense) {
        if (localExpense.id != remoteExpense.id) {
            logger.w("SyncIdHandler", "ID Mismatch for FuelExpense: ${localExpense.id} -> ${remoteExpense.id}")
            appDatabase.withTransaction {
                fuelExpenseDao.deleteExpense(localExpense.id)
                fuelExpenseDao.insertExpense(remoteExpense.copy(syncStatus = SyncStatus.SYNCED).toEntity())
            }
        } else {
            logger.d("SyncIdHandler", "ID Match for FuelExpense: ${remoteExpense.id}. Marking as SYNCED.")
            fuelExpenseDao.updateSyncStatus(remoteExpense.id, SyncStatus.SYNCED.name)
        }
    }
}
