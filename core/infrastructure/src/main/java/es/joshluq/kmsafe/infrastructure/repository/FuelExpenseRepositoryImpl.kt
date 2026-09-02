package es.joshluq.kmsafe.infrastructure.repository

import es.joshluq.foundationkit.coroutines.DispatcherProvider
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.AuthSessionState
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.domain.model.SyncStatus
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import es.joshluq.kmsafe.infrastructure.local.dao.FuelExpenseDao
import es.joshluq.kmsafe.infrastructure.local.entity.toDomain
import es.joshluq.kmsafe.infrastructure.local.entity.toEntity
import es.joshluq.kmsafe.infrastructure.mapper.toDomain as toDomainFromApi
import es.joshluq.kmsafe.infrastructure.mapper.toRequest
import es.joshluq.kmsafe.infrastructure.remote.api.FuelApiService
import es.joshluq.kmsafe.infrastructure.remote.auth.UserSessionDataSource
import es.joshluq.kmsafe.infrastructure.repository.util.SyncIdHandler
import es.joshluq.kmsafe.infrastructure.worker.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [FuelExpenseRepository] using Room local persistence.
 *
 * Follows the stateless repository pattern by delegating persistence to [FuelExpenseDao].
 *
 * @property dao Room DAO for fuel expenses.
 * @property logger Logger utility.
 * @property dispatchers Coroutine dispatcher provider.
 */
@Singleton
class FuelExpenseRepositoryImpl @Inject constructor(
    private val dao: FuelExpenseDao,
    private val apiService: FuelApiService,
    private val sessionDataSource: UserSessionDataSource,
    private val syncIdHandler: SyncIdHandler,
    private val syncManager: SyncManager,
    private val logger: LoggerKit,
    private val dispatchers: DispatcherProvider
) : FuelExpenseRepository {

    override fun getExpensesByVehicle(vehicleId: String): Flow<List<FuelExpense>> {
        return dao.getExpensesByVehicle(vehicleId).map { entities ->
            entities.map { it.toDomain() }
        }.onEach {
            logger.d("FuelExpenseRepository", "Fetched ${it.size} expenses for vehicle $vehicleId")
        }.flowOn(dispatchers.io)
    }

    override fun getExpensesByStation(stationId: String): Flow<List<FuelExpense>> {
        return dao.getExpensesByStation(stationId).map { entities ->
            entities.map { it.toDomain() }
        }.onEach {
            logger.d("FuelExpenseRepository", "Fetched ${it.size} expenses for station $stationId")
        }.flowOn(dispatchers.io)
    }

    override fun getExpenseById(id: String): Flow<FuelExpense?> {
        return dao.getExpenseById(id).map { it?.toDomain() }
            .flowOn(dispatchers.io)
    }

    override fun saveExpense(expense: FuelExpense): Flow<String> = flow {
        logger.d("FuelExpenseRepository", "DAO insert trigger for expense: ${expense.id} at ${expense.timestamp}")
        
        // 1. Local-First save
        val expenseToSave = expense.copy(syncStatus = SyncStatus.PENDING)
        dao.insertExpense(expenseToSave.toEntity())
        
        var finalId = expense.id

        // 2. Remote Sync (Creation)
        if (sessionDataSource.getSessionState().first() is AuthSessionState.Active) {
            runCatching {
                if (sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
                    val response = apiService.createFuelExpense(
                        contractId = expense.vehicleId,
                        request = expense.toRequest()
                    )
                    if (response.isSuccessful) {
                        logger.i("FuelExpenseRepository", "Remote expense creation successful")
                        val remoteExpense = response.body()?.expense?.toDomainFromApi()
                        if (remoteExpense != null) {
                            syncIdHandler.resolveFuelExpenseId(expense, remoteExpense)
                            finalId = remoteExpense.id
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: ""
                        if (errorBody.contains("duplicate key", ignoreCase = true)) {
                            logger.w("FuelExpenseRepository", "Remote sync conflict: Duplicate key. Marking as SYNCED.")
                            dao.insertExpense(expense.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                        } else {
                            syncManager.scheduleSync()
                        }
                    }
                }
            }.onFailure { e ->
                logger.e("FuelExpenseRepository", "Remote expense creation failed", e)
                syncManager.scheduleSync()
            }
        }
        
        emit(finalId)
    }.flowOn(dispatchers.io)

    override fun updateExpense(expense: FuelExpense): Flow<Unit> = flow {
        logger.d("FuelExpenseRepository", "Updating expense: ${expense.id}")

        // 1. Local-First update
        val expenseToUpdate = expense.copy(
            syncStatus = SyncStatus.PENDING,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertExpense(expenseToUpdate.toEntity())

        // 2. Remote Sync (Update)
        if (sessionDataSource.getSessionState().first() is AuthSessionState.Active) {
            runCatching {
                if (sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
                    val response = apiService.updateFuelExpense(
                        id = expense.id,
                        request = expense.toRequest()
                    )
                    if (response.isSuccessful) {
                        logger.i("FuelExpenseRepository", "Remote expense update successful")
                        val remoteExpense = response.body()?.expense?.toDomainFromApi()
                        if (remoteExpense != null) {
                            syncIdHandler.resolveFuelExpenseId(expense, remoteExpense)
                        }
                    } else {
                        syncManager.scheduleSync()
                    }
                }
            }.onFailure { e ->
                logger.e("FuelExpenseRepository", "Remote expense update failed", e)
                syncManager.scheduleSync()
            }
        }

        emit(Unit)
    }.flowOn(dispatchers.io)

    override fun deleteExpense(id: String): Flow<Unit> = flow {
        logger.d("FuelExpenseRepository", "Deleting expense ID: $id")
        
        // 1. Local delete
        dao.deleteExpense(id)

        // 2. Remote delete
        if (sessionDataSource.getSessionState().first() is AuthSessionState.Active) {
            if (sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
                runCatching {
                    apiService.deleteFuelExpense(id)
                }.onFailure {
                    syncManager.scheduleSync()
                }
            }
        }
        
        emit(Unit)
    }.flowOn(dispatchers.io)

    override fun deleteExpensesByVehicle(vehicleId: String): Flow<Unit> = flow {
        logger.d("FuelExpenseRepository", "Deleting all expenses for vehicle ID: $vehicleId")
        dao.deleteExpensesByVehicle(vehicleId)
        emit(Unit)
    }.flowOn(dispatchers.io)

    override fun clearAllExpenses(): Flow<Unit> = flow {
        logger.i("FuelExpenseRepository", "Clearing all fuel expenses")
        dao.clearAllExpenses()
        emit(Unit)
    }.flowOn(dispatchers.io)

    override fun syncFuelExpenses(vehicleId: String): Flow<List<FuelExpense>> = flow {
        logger.d("FuelExpenseRepository", "Starting fuel expense synchronization for $vehicleId")

        if (!sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
            logger.d("FuelExpenseRepository", "Sync skipped: Cloud Sync feature not enabled")
            emit(emptyList())
            return@flow
        }

        val response = apiService.getFuelExpenses(vehicleId)
        if (response.isSuccessful) {
            val expenses = response.body()?.expenses?.map { it.toDomainFromApi() } ?: emptyList()
            logger.i("FuelExpenseRepository", "Sync successful: Found ${expenses.size} remote expenses")

            expenses.forEach { expense ->
                dao.insertExpense(expense.copy(syncStatus = SyncStatus.SYNCED).toEntity())
            }
            emit(expenses)
        } else {
            logger.e("FuelExpenseRepository", "Sync failed with HTTP error: ${response.code()}")
            throw KmException(KmError.NetworkError)
        }
    }.flowOn(dispatchers.io)
}
