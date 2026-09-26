# Component Interface Specification: Optimización Local-First en el Registro de Odómetro

**Feature ID**: KILOMENOS-16  
**Component Identifier**: HistoryRepositoryImpl / AddOdometerRecordUseCase / OverviewViewModel  
**Package**: es.joshluq.kmsafe.infrastructure.repository / es.joshluq.kmsafe.domain.usecase / es.joshluq.kmsafe.feature.overview  
**Target Modules**: :core:domain, :core:infrastructure, :feature:overview  
**Architecture Pattern**: Clean Architecture + Local-First Optimistic UDF  
**Status**: APPROVED  

---

## 1. Domain & UseCase Layer Contract

### 1.1 `AddOdometerRecordUseCase` Interface & Implementation
El caso de uso de dominio valida el contrato activo, construye la entidad de dominio con su UUID inmutable y delega la persistencia local al repositorio, emitiendo `Success` inmediatamente tras la confirmación de Room.

```kotlin
package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.TripRoute
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import java.util.UUID
import javax.inject.Inject

interface AddOdometerRecordUseCase : FlowUseCase<AddOdometerRecordUseCase.Input, AddOdometerRecordUseCase.Output> {

    data class Input(
        val odometerValue: Double,
        val timestamp: Long,
        val label: String? = null,
        val fuelAmount: Double? = null,
        val encodedPolyline: String? = null,
        val pointCount: Int? = null
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Failure(val message: String) : Output
        data class Success(val recordId: String) : Output
    }
}

class AddOdometerRecordUseCaseImpl @Inject constructor(
    private val rentingRepository: RentingRepository,
    private val historyRepository: HistoryRepository
) : AddOdometerRecordUseCase {

    override fun invoke(input: AddOdometerRecordUseCase.Input): Flow<AddOdometerRecordUseCase.Output> = flow {
        if (input.odometerValue <= 0.0) {
            emit(AddOdometerRecordUseCase.Output.Failure("El kilometraje debe ser mayor a 0"))
            return@flow
        }

        val contract = rentingRepository.getContract().firstOrNull()
        if (contract == null) {
            emit(AddOdometerRecordUseCase.Output.Failure("No renting contract found"))
            return@flow
        }

        val record = OdometerRecord(
            id = UUID.randomUUID().toString(),
            contractId = contract.id,
            timestamp = input.timestamp,
            odometerValue = input.odometerValue,
            isInitialRecord = false,
            label = input.label,
            fuelAmount = input.fuelAmount,
            hasRoute = input.encodedPolyline != null
        )

        val route = input.encodedPolyline?.let {
            TripRoute(
                recordId = record.id,
                encodedPolyline = it,
                pointCount = input.pointCount ?: 0
            )
        }

        // Local-First: Persists to Room immediately and triggers background sync asynchronously
        historyRepository.saveRecord(record, route)
        emit(AddOdometerRecordUseCase.Output.Success(record.id))
    }
        .onStart { emit(AddOdometerRecordUseCase.Output.Progress) }
        .catch { emit(AddOdometerRecordUseCase.Output.Failure(it.message ?: "Unknown error")) }
}
```

---

## 2. Infrastructure & Repository Layer Contract

### 2.1 `HistoryRepositoryImpl.kt` — Persistencia Local Inmediata y Sync Desacoplado

```kotlin
package es.joshluq.kmsafe.infrastructure.repository

// ... imports ...

class HistoryRepositoryImpl @Inject constructor(
    private val dao: OdometerRecordDao,
    private val routeDao: TripRouteDao,
    private val appDatabase: AppDatabase,
    private val apiService: RentingApiService,
    private val sessionDataSource: UserSessionDataSource,
    private val syncManager: SyncManager,
    private val errorMapper: ErrorMapper,
    private val logger: LoggerKit,
    private val dispatchers: DispatcherProvider,
    private val externalScope: CoroutineScope // Application-scoped coroutine runner
) : HistoryRepository {

    override suspend fun saveRecord(record: OdometerRecord, route: TripRoute?) = withContext(dispatchers.io) {
        logger.d("HistoryRepository", "Saving record locally: ${record.odometerValue} km (ID: ${record.id})")

        // 1. Escritura inmediata en Room DB (SyncStatus.PENDING)
        val recordToSave = record.copy(syncStatus = SyncStatus.PENDING)
        appDatabase.withTransaction {
            dao.insertRecord(recordToSave.toEntity())
            route?.let {
                routeDao.insertRoute(it.copy(recordId = record.id).toEntity())
            }
        }

        // 2. Desacoplamiento de la sincronización remota: No bloquea el retorno a la UI
        if (!record.isInitialRecord) {
            externalScope.launch(dispatchers.io) {
                dispatchRemoteSync(recordToSave, route)
            }
        }
    }

    private suspend fun dispatchRemoteSync(record: OdometerRecord, route: TripRoute?) {
        val sessionState = sessionDataSource.getSessionState().first()
        if (sessionState !is AuthSessionState.Active || !sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
            logger.d("HistoryRepository", "Remote sync postponed: Session inactive or Cloud Sync not enabled")
            return
        }

        runCatching {
            val response = apiService.addOdometerRecord(
                contractId = record.contractId,
                request = AddOdometerRecordRequest(
                    id = record.id,
                    timestamp = record.timestamp.toIsoString(),
                    odometerValue = record.odometerValue,
                    label = record.label,
                    fuelConsumed = record.fuelAmount
                )
            )

            if (response.isSuccessful) {
                logger.i("HistoryRepository", "Remote record creation succeeded for ${record.id}")
                dao.insertRecord(record.copy(syncStatus = SyncStatus.SYNCED).toEntity())

                if (route != null || record.hasRoute) {
                    val routeToSync = route ?: routeDao.getRouteByRecordIdSync(record.id)?.toDomainFromEntity()
                    routeToSync?.let { saveRoute(it) }
                }
            } else {
                val code = response.code()
                val errorBody = response.errorBody()?.string() ?: ""
                logger.w("HistoryRepository", "Remote sync returned $code: $errorBody. Scheduling WorkManager retry.")
                if (code == 409 || errorBody.contains("duplicate", ignoreCase = true)) {
                    dao.insertRecord(record.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                } else {
                    syncManager.scheduleSync()
                }
            }
        }.onFailure { e ->
            if (e is kotlinx.coroutines.CancellationException) throw e
            logger.e("HistoryRepository", "Remote sync failed. Scheduling WorkManager retry.", e)
            syncManager.scheduleSync()
        }
    }

    override suspend fun deleteRecord(record: OdometerRecord) = withContext(dispatchers.io) {
        logger.d("HistoryRepository", "Deleting record ID: ${record.id} (Status: ${record.syncStatus})")
        
        // Purgar de Room DB
        dao.deleteRecord(record.toEntity())

        // Outbox Optimization: Si nunca llegó a sincronizarse, no enviar DELETE al servidor (evita 404)
        if (record.syncStatus == SyncStatus.PENDING) {
            logger.d("HistoryRepository", "Record was PENDING; skipping remote DELETE")
            return@withContext
        }

        // Si ya estaba sincronizado en la nube, despachar DELETE remoto en background
        if (sessionDataSource.getSessionState().first() is AuthSessionState.Active &&
            sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)
        ) {
            externalScope.launch(dispatchers.io) {
                runCatching {
                    apiService.deleteOdometerRecord(record.id)
                }.onFailure { logger.e("HistoryRepository", "Error deleting remote record ${record.id}", it) }
            }
        }
    }
}
```

---

## 3. Presentation Layer Interaction (`OverviewViewModel`)

En `OverviewViewModel.handleSaveRecord(timestamp)`:
1. Al recibir `AddOdometerRecordUseCase.Output.Success`:
   - `updateState { copy(isSaving = false, showBottomSheet = false, isLoading = false) }` se ejecuta en menos de **30ms**.
   - Se limpian los estados de auto-tracking y notificaciones activas.
   - El `Flow` de Room DB emite los nuevos registros automáticamente, provocando que `calculateContractMetricsUseCase` actualice en el acto las variables del estado:
     - `totalKmsDriven`
     - `currentOdometer`
     - `balance`
     - `projection`
2. El usuario no experimenta ninguna barra de progreso o bloqueo mientras la app sincroniza con la nube en segundo plano.
