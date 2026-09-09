package es.joshluq.kmsafe.domain.model

/**
 * Represents the global state of an active fleet vehicle transition.
 *
 * @property isSwitching True when a vehicle switch operation is currently in progress.
 * @property vehicleName The display name of the target vehicle being switched to.
 */
data class FleetSwitchingState(
    val isSwitching: Boolean = false,
    val vehicleName: String? = null
)
