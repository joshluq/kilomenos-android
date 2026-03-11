package es.joshluq.kmsafe.data.location

import es.joshluq.kmsafe.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingRepositoryImpl @Inject constructor() : TrackingRepository {

    private val _currentDistanceMeters = MutableStateFlow(0.0)
    override val currentDistanceMeters: StateFlow<Double> = _currentDistanceMeters.asStateFlow()

    private val _startTime = MutableStateFlow<Long?>(null)
    override val startTime: StateFlow<Long?> = _startTime.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    override val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    override fun startTracking() {
        _currentDistanceMeters.value = 0.0
        _startTime.value = System.currentTimeMillis()
        _isTracking.value = true
    }

    override fun updateDistance(meters: Double) {
        if (_isTracking.value) {
            _currentDistanceMeters.value += meters
        }
    }

    override fun stopTracking() {
        _isTracking.value = false
        // We don't reset meters immediately so the UI can show the final result before saving
    }

    override fun clear() {
        _isTracking.value = false
        _currentDistanceMeters.value = 0.0
        _startTime.value = null
    }
}
