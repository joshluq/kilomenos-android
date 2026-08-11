package es.joshluq.kmsafe.data.local.datasource

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.provider.StorageProvider
import es.joshluq.foundationkit.provider.read
import es.joshluq.foundationkit.provider.save
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataSource for tracking state persistence using Secure StorageProvider.
 * This ensures that active trips survive service restarts.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class TrackingDataSource @Inject constructor(
    private val storage: StorageProvider,
    private val logger: LoggerKit
) {
    private val _updates = MutableSharedFlow<Unit>(replay = 1).apply { 
        tryEmit(Unit) 
    }

    companion object {
        private const val KEY_IS_TRACKING = "tracking_active"
        private const val KEY_START_TIME = "tracking_start_timestamp"
        private const val KEY_DISTANCE = "tracking_distance_meters"
    }

    fun isTracking(): Flow<Boolean> = _updates.flatMapLatest {
        flow { emit(storage.read<Boolean>(KEY_IS_TRACKING) ?: false) }
    }

    fun getStartTime(): Flow<Long?> = _updates.flatMapLatest {
        flow { emit(storage.read<Long>(KEY_START_TIME)) }
    }

    fun getDistanceMeters(): Flow<Double> = _updates.flatMapLatest {
        flow { emit(storage.read<Double>(KEY_DISTANCE) ?: 0.0) }
    }

    suspend fun startTracking(timestamp: Long) {
        storage.save(KEY_IS_TRACKING, true)
        storage.save(KEY_START_TIME, timestamp)
        storage.save(KEY_DISTANCE, 0.0)
        _updates.emit(Unit)
    }

    suspend fun updateDistance(meters: Double) {
        val current = storage.read<Double>(KEY_DISTANCE) ?: 0.0
        storage.save(KEY_DISTANCE, current + meters)
        _updates.emit(Unit)
    }

    suspend fun stopTracking() {
        logger.d("TrackingDataSource", "stopTracking: setting IS_TRACKING to false")
        storage.save(KEY_IS_TRACKING, false)
        _updates.emit(Unit)
    }

    suspend fun clear() {
        logger.d("TrackingDataSource", "clear: deleting tracking keys")
        storage.delete(KEY_IS_TRACKING)
        storage.delete(KEY_START_TIME)
        storage.delete(KEY_DISTANCE)
        _updates.emit(Unit)
    }
}
