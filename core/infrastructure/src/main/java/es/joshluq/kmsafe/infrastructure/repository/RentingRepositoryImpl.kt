package es.joshluq.kmsafe.infrastructure.repository

import androidx.room.withTransaction
import es.joshluq.kmsafe.domain.model.AuthSessionState
import es.joshluq.foundationkit.coroutines.DispatcherProvider
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.infrastructure.InfrastructureConfig
import es.joshluq.kmsafe.infrastructure.local.AppDatabase
import es.joshluq.kmsafe.infrastructure.local.dao.OdometerRecordDao
import es.joshluq.kmsafe.infrastructure.local.dao.RentingContractDao
import es.joshluq.kmsafe.infrastructure.local.entity.toDomain as toDomainFromEntity
import es.joshluq.kmsafe.infrastructure.local.entity.toEntity
import es.joshluq.kmsafe.infrastructure.mapper.ErrorMapper
import es.joshluq.kmsafe.infrastructure.mapper.toDomain as toDomainFromApi
import es.joshluq.kmsafe.infrastructure.mapper.toIsoString
import es.joshluq.kmsafe.infrastructure.remote.api.RentingApiService
import es.joshluq.kmsafe.infrastructure.remote.api.StorageApiService
import es.joshluq.kmsafe.infrastructure.remote.auth.UserSessionDataSource
import es.joshluq.kmsafe.infrastructure.remote.request.CreateRentingContractRequest
import es.joshluq.kmsafe.infrastructure.remote.request.UpdateRentingContractRequest
import es.joshluq.kmsafe.infrastructure.repository.util.SyncIdHandler
import es.joshluq.kmsafe.infrastructure.worker.SyncManager
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.SyncStatus
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

/**
 * Implementation of [RentingRepository] using Room and Remote API.
 */
class RentingRepositoryImpl @Inject constructor(
    private val rentingDao: RentingContractDao,
    private val odometerDao: OdometerRecordDao,
    private val appDatabase: AppDatabase,
    private val apiService: RentingApiService,
    private val storageApiService: StorageApiService,
    private val sessionDataSource: UserSessionDataSource,
    private val syncIdHandler: SyncIdHandler,
    private val syncManager: SyncManager,
    private val errorMapper: ErrorMapper,
    private val logger: LoggerKit,
    private val dispatchers: DispatcherProvider,
    private val config: InfrastructureConfig
) : RentingRepository {

    override fun saveContract(contract: RentingContract): Flow<String> = flow {
        logger.d("RentingRepository", "Saving contract for vehicle: ${contract.vehicleName}")

        // 1. Local-First save. Always PENDING.
        val contractToSave = contract.copy(syncStatus = SyncStatus.PENDING)
        rentingDao.insertContract(contractToSave.toEntity())

        var finalId = contract.id

        // 2. Remote Sync (Only if session is active and user has Cloud Sync feature)
        if (sessionDataSource.getSessionState().first() is AuthSessionState.Active) {
            runCatching {
                if (sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
                    val request = CreateRentingContractRequest(
                        id = contract.id,
                        vehicleName = contract.vehicleName,
                        startDate = contract.startDate.toIsoString(),
                        durationMonths = contract.durationMonths,
                        totalKms = contract.totalKms,
                        startOdometer = contract.startOdometer,
                        currentOdometer = contract.currentOdometer,
                        isSelected = contract.isSelected,
                        vehicleImageUrl = contract.vehicleImageUrl,
                        bluetoothDeviceName = contract.bluetoothDeviceName,
                        bluetoothDeviceAddress = contract.bluetoothDeviceAddress,
                        excessKmPrice = contract.excessDistancePrice,
                        courtesyKmBuffer = contract.courtesyMarginKms
                    )
                    val response = apiService.createContract(request)
                    if (response.isSuccessful) {
                        logger.i("RentingRepository", "Remote contract sync successful")
                        val remoteContract = response.body()?.contract?.toDomainFromApi()
                        if (remoteContract != null) {
                            syncIdHandler.resolveRentingId(contract, remoteContract)
                            finalId = remoteContract.id
                        }
                    } else {
                        syncManager.scheduleSync()
                    }
                }
            }.onFailure {
                syncManager.scheduleSync()
            }
        }

        emit(finalId)
    }.flowOn(dispatchers.io)

    override fun updateContract(contract: RentingContract): Flow<Unit> = flow {
        logger.d("RentingRepository", "Updating contract ID: ${contract.id}")

        // 1. Local update with PENDING status
        val contractToUpdate = contract.copy(syncStatus = SyncStatus.PENDING)
        rentingDao.insertContract(contractToUpdate.toEntity())

        // 2. Remote update
        if (sessionDataSource.getSessionState().first() is AuthSessionState.Active &&
            sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)
        ) {
            runCatching {
                val request = UpdateRentingContractRequest(
                    vehicleName = contract.vehicleName,
                    startDate = contract.startDate.toIsoString(),
                    durationMonths = contract.durationMonths,
                    totalKms = contract.totalKms,
                    startOdometer = contract.startOdometer,
                    vehicleImageUrl = contract.vehicleImageUrl,
                    bluetoothDeviceName = contract.bluetoothDeviceName,
                    bluetoothDeviceAddress = contract.bluetoothDeviceAddress,
                    excessKmPrice = contract.excessDistancePrice,
                    courtesyKmBuffer = contract.courtesyMarginKms
                )
                val response = apiService.updateContract(contract.id, request)
                if (response.isSuccessful) {
                    logger.i("RentingRepository", "Remote contract update successful")
                    // Success: Mark as SYNCED locally
                    rentingDao.insertContract(contract.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                } else {
                    logger.e("RentingRepository", "Remote update failed: ${response.code()}")
                    syncManager.scheduleSync()
                }
            }.onFailure {
                logger.e("RentingRepository", "Error during remote update", it)
                syncManager.scheduleSync()
            }
        }
        emit(Unit)
    }.flowOn(dispatchers.io)

    override fun getContract(): Flow<RentingContract?> {
        return rentingDao.getSelectedContract().map { entity ->
            entity?.toDomainFromEntity()
        }.onEach {
            logger.d("RentingRepository", "Selected contract fetched: ${it?.vehicleName ?: "None"}")
        }
    }

    override fun getContractById(id: String): Flow<RentingContract?> {
        return rentingDao.getContractById(id).map { entity ->
            entity?.toDomainFromEntity()
        }.onEach {
            logger.d("RentingRepository", "Contract by ID $id fetched: ${it?.vehicleName ?: "Not found"}")
        }
    }

    override fun getAllContracts(): Flow<List<RentingContract>> {
        return rentingDao.getAllContracts().map { entities ->
            entities.map { it.toDomainFromEntity() }
        }.onEach {
            logger.d("RentingRepository", "All contracts fetched from DB: ${it.size} items")
        }
    }

    override suspend fun getPendingContracts(): List<RentingContract> {
        return rentingDao.getPendingContracts().map { it.toDomainFromEntity() }
    }

    override fun selectContract(id: String): Flow<Unit> = flow {
        logger.d("RentingRepository", "Selecting contract ID: $id")

        appDatabase.withTransaction {
            rentingDao.updateSelection(id)
            // Mark as PENDING to ensure selection state is synced if offline
            rentingDao.getContractByIdSync(id)?.let { entity ->
                rentingDao.insertContract(entity.copy(syncStatus = SyncStatus.PENDING.name))
            }
        }

        if (sessionDataSource.getSessionState().first() is AuthSessionState.Active &&
            sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)
        ) {
            logger.d("RentingRepository", "Session active and Cloud Sync enabled, attempting remote selection sync")
            runCatching {
                val response = apiService.selectContract(id)
                if (response.isSuccessful) {
                    logger.i("RentingRepository", "Remote selection sync successful")
                    appDatabase.withTransaction {
                        rentingDao.getContractByIdSync(id)?.let { entity ->
                            rentingDao.insertContract(entity.copy(syncStatus = SyncStatus.SYNCED.name))
                        }
                    }
                } else {
                    logger.e("RentingRepository", "Remote selection failed with code: ${response.code()}")
                    syncManager.scheduleSync()
                }
            }.onFailure {
                logger.e("RentingRepository", "Error during remote selection sync", it)
                syncManager.scheduleSync()
            }
        }
        emit(Unit)
    }.flowOn(dispatchers.io)

    override fun deleteContract(id: String): Flow<Unit> = flow {
        logger.d("RentingRepository", "Deleting contract ID: $id")
        odometerDao.deleteRecordsByContractId(id)
        rentingDao.deleteContract(id)

        if (sessionDataSource.getSessionState().first() is AuthSessionState.Active &&
            sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)
        ) {
            logger.d("RentingRepository", "Session active and Cloud Sync enabled, attempting remote deletion sync")
            runCatching {
                val response = apiService.deleteContract(id)
                if (response.isSuccessful) {
                    logger.i("RentingRepository", "Remote deletion sync successful")
                } else {
                    logger.e("RentingRepository", "Remote deletion failed with code: ${response.code()}")
                }
            }.onFailure {
                logger.e("RentingRepository", "Error during remote deletion sync", it)
            }
        }
        emit(Unit)
    }.flowOn(dispatchers.io)

    override fun syncContracts(): Flow<List<RentingContract>> = flow {
        logger.d("RentingRepository", "Starting contract synchronization")

        if (!sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
            logger.d("RentingRepository", "Sync skipped: Cloud Sync feature not enabled")
            emit(emptyList())
            return@flow
        }

        val response = apiService.getContracts()
        if (response.isSuccessful) {
            val contracts = response.body()?.contracts?.map { it.toDomainFromApi() } ?: emptyList()
            logger.i("RentingRepository", "Sync successful: Found ${contracts.size} remote contracts")

            contracts.forEach { contract ->
                rentingDao.insertContract(contract.toEntity())
            }
            emit(contracts)
        } else {
            logger.e("RentingRepository", "Sync failed with HTTP error: ${response.code()}")
            throw KmException(errorMapper.mapApiResponse(response))
        }
    }.flowOn(dispatchers.io)

    override fun uploadVehicleImage(imageBytes: ByteArray, fileName: String): Flow<String> = flow {
        logger.d("RentingRepository", "Uploading image: $fileName")
        val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())

        val response = storageApiService.uploadVehicleImage(fileName, requestBody)
        if (response.isSuccessful) {
            // Use config.storageUrl instead of BuildConfig.STORAGE_URL for module isolation
            val publicUrl = "${config.storageUrl}$fileName"
            logger.i("RentingRepository", "Image uploaded successfully. URL: $publicUrl")
            emit(publicUrl)
        } else {
            logger.e("RentingRepository", "Image upload failed with code: ${response.code()}")
            throw KmException(KmError.NetworkError)
        }
    }.flowOn(dispatchers.io)

    override suspend fun getDatabaseOwnerId(): String? {
        val ownerId = rentingDao.getFirstContractUserId()
        logger.d("RentingRepository", "Database owner ID check: $ownerId")
        return ownerId
    }

    override fun clearAllLocalData(): Flow<Unit> = flow {
        logger.i("RentingRepository", "Clearing all local data")
        odometerDao.clearAllRecords()
        rentingDao.clearAllContracts()
        // trip_route has CASCADE but we clear explicitly for safety
        appDatabase.tripRouteDao().clearAllRoutes()
        emit(Unit)
    }.flowOn(dispatchers.io)
}
