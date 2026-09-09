package es.joshluq.kmsafe.domain.repository

import es.joshluq.kmsafe.domain.model.AppOverlayState
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for observing and updating the global application HUD overlay state.
 */
interface AppOverlayRepository {

    /**
     * Observes the current active [AppOverlayState].
     */
    fun observeOverlay(): Flow<AppOverlayState>

    /**
     * Sets the active [AppOverlayState].
     */
    suspend fun setOverlay(state: AppOverlayState)

    /**
     * Clears the current overlay, returning to [AppOverlayState.None].
     */
    suspend fun clearOverlay()
}
