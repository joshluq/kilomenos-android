package es.joshluq.kmsafe.data.repository.util

import androidx.room.withTransaction
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.data.local.AppDatabase
import es.joshluq.kmsafe.data.local.dao.OdometerRecordDao
import es.joshluq.kmsafe.data.local.dao.RentingContractDao
import es.joshluq.kmsafe.data.local.dao.TripRouteDao
import es.joshluq.kmsafe.data.local.entity.toEntity
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RentingContract
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
                rentingDao.deleteContract(localContract.id)
                odometerDao.updateContractId(oldId = localContract.id, newId = remoteContract.id)
                
                // Persist the remote version with local UI state preserved
                val toPersist = remoteContract.copy(
                    isSelected = localContract.isSelected,
                    syncStatus = SyncStatus.SYNCED
                )
                rentingDao.insertContract(toPersist.toEntity())
            }
        } else {
            logger.d("SyncIdHandler", "ID Match for Contract: ${remoteContract.id}. Marking as SYNCED.")
            rentingDao.insertContract(remoteContract.copy(syncStatus = SyncStatus.SYNCED).toEntity())
        }
    }

    /**
     * Resolves the identity of an OdometerRecord after a remote sync.
     * Handles associated trip routes if an ID swap is required.
     */
    suspend fun resolveOdometerId(localRecord: OdometerRecord, remoteRecord: OdometerRecord) {
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
                odometerDao.insertRecord(remoteRecord.copy(syncStatus = SyncStatus.SYNCED).toEntity())
            }
        } else {
            logger.d("SyncIdHandler", "ID Match for Record: ${remoteRecord.id}. Marking as SYNCED.")
            odometerDao.insertRecord(remoteRecord.copy(syncStatus = SyncStatus.SYNCED).toEntity())
        }
    }
}
