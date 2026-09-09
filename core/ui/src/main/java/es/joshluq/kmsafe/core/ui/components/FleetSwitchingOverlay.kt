package es.joshluq.kmsafe.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.joshluq.kmsafe.domain.model.AppOverlayState

/**
 * Backwards-compatible delegate to [AppExecutiveHudOverlay] for vehicle fleet switching.
 *
 * @param isVisible Flag indicating if the switching overlay is active.
 * @param vehicleName The display name of the target vehicle being switched to.
 * @param modifier Modifier applied to the overlay container.
 */
@Composable
fun FleetSwitchingOverlay(
    isVisible: Boolean,
    vehicleName: String?,
    modifier: Modifier = Modifier
) {
    AppExecutiveHudOverlay(
        state = if (isVisible) AppOverlayState.VehicleSwitching(vehicleName) else AppOverlayState.None,
        modifier = modifier
    )
}
