package es.joshluq.kmsafe.infrastructure.repository

import es.joshluq.foundationkit.coroutines.DispatcherProvider
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.AuthSessionState
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.SyncStatus
import es.joshluq.kmsafe.infrastructure.local.AppDatabase
import es.joshluq.kmsafe.infrastructure.local.dao.OdometerRecordDao
import es.joshluq.kmsafe.infrastructure.local.dao.TripRouteDao
import es.joshluq.kmsafe.infrastructure.mapper.ErrorMapper
import es.joshluq.kmsafe.infrastructure.remote.api.RentingApiService
import es.joshluq.kmsafe.infrastructure.remote.auth.UserSessionDataSource
import es.joshluq.kmsafe.infrastructure.remote.response.AddOdometerRecordResponse
import es.joshluq.kmsafe.infrastructure.remote.response.OdometerRecordResponse
import es.joshluq.kmsafe.infrastructure.repository.util.SyncIdHandler
import es.joshluq.kmsafe.infrastructure.worker.SyncManager
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val dao: OdometerRecordDao = mockk(relaxed = true)
    private val routeDao: TripRouteDao = mockk(relaxed = true)
    private val appDatabase: AppDatabase = mockk(relaxed = true)
    private val apiService: RentingApiService = mockk(relaxed = true)
    private val sessionDataSource: UserSessionDataSource = mockk(relaxed = true)
    private val syncIdHandler: SyncIdHandler = mockk(relaxed = true)
    private val syncManager: SyncManager = mockk(relaxed = true)
    private val errorMapper: ErrorMapper = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)

    private val dispatcherProvider = object : DispatcherProvider {
        override val main = testDispatcher
        override val io = testDispatcher
        override val default = testDispatcher
        override val unconfined = testDispatcher
    }

    private lateinit var repository: HistoryRepositoryImpl

    private val testRecord = OdometerRecord(
        id = "test-uuid-1",
        contractId = "contract-1",
        timestamp = 1000L,
        odometerValue = 50.0,
        isInitialRecord = false,
        syncStatus = SyncStatus.PENDING
    )

    @Before
    fun setUp() {
        every { sessionDataSource.getSessionState() } returns flowOf(AuthSessionState.Active)
        coEvery { sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id) } returns true

        repository = HistoryRepositoryImpl(
            dao = dao,
            routeDao = routeDao,
            appDatabase = appDatabase,
            apiService = apiService,
            sessionDataSource = sessionDataSource,
            syncIdHandler = syncIdHandler,
            syncManager = syncManager,
            errorMapper = errorMapper,
            logger = logger,
            dispatchers = dispatcherProvider,
            coroutineScope = testScope
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given record when saveRecord then inserts into Room as PENDING immediately and syncs remotely in background`() = testScope.runTest {
        val remoteDto = mockk<OdometerRecordResponse> {
            every { id } returns "test-uuid-1"
            every { contractId } returns "contract-1"
            every { timestamp } returns "2026-09-26T12:00:00Z"
            every { odometerValue } returns 50.0
            every { isInitialRecord } returns false
            every { label } returns null
            every { fuelConsumed } returns null
            every { hasRoute } returns false
        }
        val addResponse = mockk<AddOdometerRecordResponse> {
            every { success } returns true
            every { record } returns remoteDto
        }
        coEvery { apiService.addOdometerRecord(any(), any()) } returns Response.success(addResponse)

        repository.saveRecord(testRecord, route = null)

        // Local insert happens immediately before remote call
        coVerify(exactly = 1) {
            dao.insertRecord(match { it.id == "test-uuid-1" && it.syncStatus == "PENDING" })
        }

        advanceUntilIdle()

        // Background sync runs on coroutineScope
        coVerify(exactly = 1) {
            apiService.addOdometerRecord("contract-1", any())
        }
    }

    @Test
    fun `given PENDING record when deleteRecord then deletes from Room and skips remote DELETE API`() = testScope.runTest {
        val pendingRecord = testRecord.copy(syncStatus = SyncStatus.PENDING)

        repository.deleteRecord(pendingRecord)

        coVerify(exactly = 1) { dao.deleteRecord(match { it.id == pendingRecord.id }) }
        advanceUntilIdle()
        coVerify(exactly = 0) { apiService.deleteOdometerRecord(any()) }
    }

    @Test
    fun `given SYNCED record when deleteRecord then deletes from Room and calls remote DELETE API`() = testScope.runTest {
        val syncedRecord = testRecord.copy(syncStatus = SyncStatus.SYNCED)
        coEvery { apiService.deleteOdometerRecord("test-uuid-1") } returns Response.success(Unit)

        repository.deleteRecord(syncedRecord)

        coVerify(exactly = 1) { dao.deleteRecord(match { it.id == syncedRecord.id }) }
        advanceUntilIdle()
        coVerify(exactly = 1) { apiService.deleteOdometerRecord("test-uuid-1") }
    }

    @Test
    fun `given record when updateRecord then updates in Room as PENDING and updates remotely`() = testScope.runTest {
        coEvery { apiService.updateOdometerRecord(any(), any()) } returns Response.success(Unit)

        repository.updateRecord(testRecord)

        coVerify(exactly = 1) {
            dao.insertRecord(match { it.id == "test-uuid-1" && it.syncStatus == "PENDING" })
        }
        advanceUntilIdle()
        coVerify(exactly = 1) {
            apiService.updateOdometerRecord("test-uuid-1", any())
        }
        coVerify(exactly = 1) {
            dao.insertRecord(match { it.id == "test-uuid-1" && it.syncStatus == "SYNCED" })
        }
    }

    @Test
    fun `given remote sync network failure when saveRecord then schedules retry via syncManager`() = testScope.runTest {
        coEvery { apiService.addOdometerRecord(any(), any()) } throws RuntimeException("Network timeout")

        repository.saveRecord(testRecord, route = null)
        advanceUntilIdle()

        coVerify(exactly = 1) { syncManager.scheduleSync() }
    }
}
