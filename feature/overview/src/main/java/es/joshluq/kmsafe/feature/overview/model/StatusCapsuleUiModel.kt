package es.joshluq.kmsafe.feature.overview.model

import androidx.compose.runtime.Immutable
import es.joshluq.foundationkit.text.TextProvider

/**
 * Priority levels for the unified status capsule.
 * Lower value represents higher cognitive priority.
 */
enum class CapsulePriority(val order: Int) {
    CRITICAL_RISK(1),
    BLUETOOTH_SETUP(2),
    FLEET_NOTICE(3),
    INSIGHT(4)
}

/**
 * Visual styling variants for the unified status capsule.
 */
enum class CapsuleVariant {
    WARNING,
    INFO,
    SUCCESS,
    NEUTRAL
}

/**
 * Sealed class representing an item displayed in the unified status capsule.
 */
@Immutable
sealed class StatusCapsuleUiModel(
    val priority: CapsulePriority,
    val variant: CapsuleVariant,
    open val message: TextProvider
) {
    data class CriticalRisk(
        override val message: TextProvider,
        val isOverLimit: Boolean = true
    ) : StatusCapsuleUiModel(
        priority = CapsulePriority.CRITICAL_RISK,
        variant = if (isOverLimit) CapsuleVariant.WARNING else CapsuleVariant.SUCCESS,
        message = message
    )

    data class BluetoothMissing(
        override val message: TextProvider
    ) : StatusCapsuleUiModel(
        priority = CapsulePriority.BLUETOOTH_SETUP,
        variant = CapsuleVariant.INFO,
        message = message
    )

    data class FleetNotice(
        override val message: TextProvider
    ) : StatusCapsuleUiModel(
        priority = CapsulePriority.FLEET_NOTICE,
        variant = CapsuleVariant.INFO,
        message = message
    )

    data class DrivingInsight(
        override val message: TextProvider
    ) : StatusCapsuleUiModel(
        priority = CapsulePriority.INSIGHT,
        variant = CapsuleVariant.NEUTRAL,
        message = message
    )
}
