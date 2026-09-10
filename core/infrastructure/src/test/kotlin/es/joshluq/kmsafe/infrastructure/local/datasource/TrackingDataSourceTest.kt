package es.joshluq.kmsafe.infrastructure.local.datasource

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.provider.StorageProvider
import es.joshluq.foundationkit.provider.read
import es.joshluq.foundationkit.provider.save
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrackingDataSourceTest {

    private val storage: StorageProvider = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var dataSource: TrackingDataSource

    @Before
    fun setUp() {
        dataSource = TrackingDataSource(storage, logger)
    }

    @Test
    fun `startTracking with no prior distance initializes fresh session`() = runTest {
        coEvery { storage.read<Double>("tracking_distance_meters") } returns null
        coEvery { storage.read<Long>("tracking_start_timestamp") } returns null

        dataSource.startTracking(1000L)

        coVerify { storage.save("tracking_active", true) }
        coVerify { storage.save("tracking_start_timestamp", 1000L) }
        coVerify { storage.save("tracking_distance_meters", 0.0) }
        coVerify { storage.delete("tracking_route_polyline") }
        coVerify { storage.save("tracking_point_count", 0) }
    }

    @Test
    fun `startTracking with existing distance resumes session without resetting metrics`() = runTest {
        coEvery { storage.read<Double>("tracking_distance_meters") } returns 15000.0
        coEvery { storage.read<Long>("tracking_start_timestamp") } returns 500L

        dataSource.startTracking(2000L)

        // Verifies tracking is activated
        coVerify { storage.save("tracking_active", true) }

        // Verifies metrics were NOT overwritten to 0 or deleted
        coVerify(exactly = 0) { storage.save("tracking_distance_meters", 0.0) }
        coVerify(exactly = 0) { storage.save("tracking_start_timestamp", 2000L) }
        coVerify(exactly = 0) { storage.delete("tracking_route_polyline") }
    }

    @Test
    fun `clear deletes all tracking keys from storage`() = runTest {
        dataSource.clear()

        coVerify { storage.delete("tracking_active") }
        coVerify { storage.delete("tracking_start_timestamp") }
        coVerify { storage.delete("tracking_distance_meters") }
        coVerify { storage.delete("tracking_route_polyline") }
        coVerify { storage.delete("tracking_point_count") }
    }
}
