package es.joshluq.kmsafe.infrastructure.local.datasource

import es.joshluq.kmsafe.domain.model.AppOverlayState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory DataSource holding the active application-wide HUD overlay state.
 * Keeping this inside a dedicated DataSource adheres to the Stateless Repository rule (AGENTS.md Rule 8).
 */
@Singleton
class AppOverlayDataSource @Inject constructor() {

    private val _overlayState = MutableStateFlow<AppOverlayState>(AppOverlayState.None)

    fun observeOverlay(): Flow<AppOverlayState> = _overlayState.asStateFlow()

    fun updateOverlay(state: AppOverlayState) {
        val current = _overlayState.value
        if (state is AppOverlayState.SubscriptionDowngraded &&
            (current is AppOverlayState.LoggingOut || current is AppOverlayState.AccountDeletion)
        ) {
            return
        }
        _overlayState.value = state
    }
}
