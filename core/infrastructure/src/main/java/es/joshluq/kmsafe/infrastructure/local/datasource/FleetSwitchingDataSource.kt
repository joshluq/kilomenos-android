package es.joshluq.kmsafe.infrastructure.local.datasource

import es.joshluq.kmsafe.domain.model.FleetSwitchingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory DataSource holding the active vehicle/fleet switching state.
 * Keeping this inside a dedicated DataSource adheres to the Stateless Repository rule (AGENTS.md Rule 8).
 */
@Singleton
class FleetSwitchingDataSource @Inject constructor() {

    private val _switchingState = MutableStateFlow(FleetSwitchingState())

    fun observeSwitchingState(): Flow<FleetSwitchingState> = _switchingState.asStateFlow()

    fun updateSwitchingState(state: FleetSwitchingState) {
        _switchingState.value = state
    }
}
