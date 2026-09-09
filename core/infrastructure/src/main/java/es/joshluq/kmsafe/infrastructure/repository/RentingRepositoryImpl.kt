package es.joshluq.kmsafe.infrastructure.repository

import androidx.room.withTransaction
import es.joshluq.kmsafe.domain.model.AuthSessionState
import es.joshluq.foundationkit.coroutines.DispatcherProvider
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.infrastructure.InfrastructureConfig
import es.joshluq.kmsafe.infrastructure.local.AppDatabase
import es.joshluq.kmsafe.infrastructure.local.dao.FuelExpenseDao
import es.joshluq.kmsafe.infrastructure.local.dao.OdometerRecordDao
import es.joshluq.kmsafe.infrastructure.local.dao.RentingContractDao
import es.joshluq.kmsafe.infrastructure.local.dao.ServiceStationDao
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
import es.joshluq.kmsafe.domain.model.FleetSwitchingState
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.SyncStatus
import es.joshluq.kmsafe.domain.repository.RentingRepository
import es.joshluq.kmsafe.infrastructure.local.datasource.FleetSwitchingDataSource
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
    private val fuelExpenseDao: FuelExpenseDao,
    private val stationDao: ServiceStationDao,
    private val appDatabase: AppDatabase,
    private val apiService: RentingApiService,
    private val storageApiService: StorageApiService,
    private val sessionDataSource: UserSessionDataSource,
    private val fleetSwitchingDataSource: FleetSwitchingDataSource,
    private val syncIdHandler: SyncIdHandler,
    private val syncManager: SyncManager,
    private val errorMapper: ErrorMapper,
    private val logger: LoggerKit,
    private val dispatchers: DispatcherProvider,
    private val config: InfrastructureConfig
) : RentingRepository {

    override fun saveContract(contract: RentingContract): Flow<String> = flow {
        logger.d("RentingRepository", "Saving contract for vehicle: ${contract.vehicleName}")

        // 1. Local-First save. Always PENDING. Use safe upsert to avoid CASCADE deletion.
        val contractToSave = contract.copy(syncStatus = SyncStatus.PENDING)
        val entity = contractToSave.toEntity()
        if (rentingDao.updateContract(entity) == 0) {
            rentingDao.insertContract(entity)
        }

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
                        courtesyKmBuffer = contract.courtesyMarginKms,
                        fuelType = contract.fuelType.name
                    )
                    val response = apiService.createContract(request)
                    if (response.isSuccessful) {
                        logger.i("RentingRepository", "Remote contract sync successful")
                        val body = response.body()
                        val remoteContract = body?.contract?.toDomainFromApi()
                        val remoteInitialRecord = body?.initialRecord?.toDomainFromApi()

                        if (remoteContract != null) {
                            syncIdHandler.resolveRentingId(contract, remoteContract)
                            finalId = remoteContract.id

                            // Reconcile initial odometer record ID if provided
                            if (remoteInitialRecord != null) {
                                logger.d("RentingRepository", "Reconciling initial record ID: ${remoteInitialRecord.id}")
                                // Find the local initial record for this contract
                                val localRecords = odometerDao.getRecordsByContractIdSync(contract.id)
                                val localInitial = localRecords.find { it.isInitialRecord }?.toDomainFromEntity()
                                if (localInitial != null) {
                                    syncIdHandler.resolveOdometerId(localInitial, remoteInitialRecord)
                                }
                            }
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: ""
                        logger.e("RentingRepository", "Remote contract creation failed with code ${response.code()}: $errorBody")
                        if (response.code() == 409 || errorBody.contains("duplicate key", ignoreCase = true) || errorBody.contains("already exists", ignoreCase = true)) {
                            logger.w("RentingRepository", "Remote sync conflict: Duplicate key or already exists. Marking as SYNCED.")
                            rentingDao.updateSyncStatus(contract.id, SyncStatus.SYNCED.name)
                        } else {
                            syncManager.scheduleSync()
                        }
                    }
                }
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                logger.e("RentingRepository", "Error during remote contract save", e)
                syncManager.scheduleSync()
            }
        }

        emit(finalId)
    }.flowOn(dispatchers.io)

    override fun updateContract(contract: RentingContract): Flow<Unit> = flow {
        logger.d("RentingRepository", "Updating contract ID: ${contract.id}")

        // 1. Local update with PENDING status. Use updateContract to avoid CASCADE deletion.
        val contractToUpdate = contract.copy(syncStatus = SyncStatus.PENDING)
        rentingDao.updateContract(contractToUpdate.toEntity())

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
                    courtesyKmBuffer = contract.courtesyMarginKms,
                    fuelType = contract.fuelType.name
                )
                val response = apiService.updateContract(contract.id, request)
                if (response.isSuccessful) {
                    logger.i("RentingRepository", "Remote contract update successful")
                    // Success: Mark as SYNCED locally. Use partial update to avoid CASCADE.
                    rentingDao.updateSyncStatus(contract.id, SyncStatus.SYNCED.name)
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    logger.e("RentingRepository", "Remote update failed with code ${response.code()}: $errorBody")
                    syncManager.scheduleSync()
                }
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                logger.e("RentingRepository", "Error during remote update", e)
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
            // Mark as PENDING to ensure selection state is synced if offline.
            // Use partial update to avoid REPLACE and its CASCADE deletion trigger.
            rentingDao.updateSyncStatus(id, SyncStatus.PENDING.name)
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
                        rentingDao.updateSyncStatus(id, SyncStatus.SYNCED.name)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    logger.e("RentingRepository", "Remote selection failed with code ${response.code()}: $errorBody")
                    syncManager.scheduleSync()
                }
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                logger.e("RentingRepository", "Error during remote selection sync", e)
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
                    val errorBody = response.errorBody()?.string() ?: ""
                    logger.e("RentingRepository", "Remote deletion failed with code ${response.code()}: $errorBody")
                }
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                logger.e("RentingRepository", "Error during remote deletion sync", e)
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
                // Use a safe upsert: try Update first, then Insert if it doesn't exist.
                // This prevents REPLACE from triggering CASCADE deletes on fuel expenses.
                val entity = contract.toEntity()
                val updatedRows = rentingDao.updateContract(entity)
                if (updatedRows == 0) {
                    rentingDao.insertContract(entity)
                }
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

        // Use x-upsert header to overwrite existing image if needed
        val response = storageApiService.uploadVehicleImage(fileName, requestBody, upsert = "true")
        
        val code = response.code()
        val isSuccessful = response.isSuccessful
        val errorBody = if (!isSuccessful) response.errorBody()?.string() ?: "" else ""

        if (isSuccessful || errorBody.contains("KeyAlreadyExists", ignoreCase = true) || code == 409) {
            if (!isSuccessful) {
                logger.w("RentingRepository", "Remote sync conflict (Image): Duplicate key or KeyAlreadyExists. Proceeding.")
            } else {
                logger.i("RentingRepository", "Image uploaded successfully.")
            }
            
            val publicUrl = "${config.storageUrl}$fileName"
            emit(publicUrl)
        } else {
            logger.e("RentingRepository", "Image upload failed with code: $code and body: $errorBody")
            throw KmException(KmError.NetworkError)
        }
    }.flowOn(dispatchers.io)

    override suspend fun getDatabaseOwnerId(): String? {
        val ownerId = rentingDao.getFirstContractUserId()
        logger.d("RentingRepository", "Database owner ID check: $ownerId")
        return ownerId
    }

    override suspend fun hasLocalData(): Boolean {
        val contracts = rentingDao.getContractCount()
        val records = odometerDao.getRecordCount()
        val expenses = fuelExpenseDao.getExpenseCount()
        val stations = stationDao.getStationCount()
        
        logger.d("RentingRepository", "Local data check: $contracts contracts, $records records, $expenses expenses, $stations stations")
        return contracts > 0 || records > 0 || expenses > 0 || stations > 0
    }

    override fun clearAllLocalData(): Flow<Unit> = flow {
        logger.i("RentingRepository", "Clearing all local data")
        odometerDao.clearAllRecords()
        rentingDao.clearAllContracts()
        fuelExpenseDao.clearAllExpenses()
        stationDao.clearAllStations()
        appDatabase.tripRouteDao().clearAllRoutes()
        emit(Unit)
    }.flowOn(dispatchers.io)

    override fun observeFleetSwitching(): Flow<FleetSwitchingState> =
        fleetSwitchingDataSource.observeSwitchingState()

    override suspend fun setFleetSwitching(state: FleetSwitchingState) {
        fleetSwitchingDataSource.updateSwitchingState(state)
    }
}
