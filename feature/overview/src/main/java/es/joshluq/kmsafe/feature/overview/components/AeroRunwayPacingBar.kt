package es.joshluq.kmsafe.feature.overview.components

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.cards.CanvasKitCardVariant
import es.joshluq.canvaskit.components.menus.CanvasKitDropdownMenu
import es.joshluq.canvaskit.components.menus.CanvasKitDropdownMenuItem
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.util.NumberFormatter
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.core.ui.util.safeClickable
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.feature.overview.R
import kotlin.math.absoluteValue

/**
 * The Hero Runway Pacing Bar Component (Doble Horizonte de Precisión).
 * Visualizes the contract horizon comparing elapsed time against real distance consumption,
 * indicating real surplus/deficit at a glance in < 2 seconds.
 *
 * @param vehicleName Name of the active vehicle (serves as entry point to vehicle details).
 * @param timePercentage Ratio of elapsed contract days [0.0f … 1.0f] or percentage.
 * @param kmsPercentage Ratio of consumed contract distance [0.0f … 1.0f] or percentage.
 * @param balance Updated balance in km (positive = surplus, negative = penalty risk).
 * @param currentOdometer Current total vehicle odometer.
 * @param daysRemaining Days remaining in the active contract.
 * @param onVehicleDetailClick Callback triggered when clicking vehicle identity to navigate to vehicle details.
 * @param modifier Modifier for card layout.
 * @param differencePercentage Pre-calculated contract difference percentage with high precision.
 * @param availableVehicles List of user vehicles for the fleet switcher.
 * @param showVehicleSwitcher Flag indicating if the fleet switcher menu is open.
 * @param onToggleVehicleSwitcher Callback to toggle fleet switcher menu visibility.
 * @param onSwitchVehicle Callback when user selects another vehicle.
 */
@Composable
fun AeroRunwayPacingBar(
    vehicleName: String,
    timePercentage: Float,
    kmsPercentage: Float,
    balance: Double,
    currentOdometer: Double,
    daysRemaining: Int,
    onVehicleDetailClick: () -> Unit,
    modifier: Modifier = Modifier,
    differencePercentage: Float? = null,
    availableVehicles: List<RentingContract> = emptyList(),
    showVehicleSwitcher: Boolean = false,
    onToggleVehicleSwitcher: () -> Unit = {},
    onSwitchVehicle: (String) -> Unit = {}
) {
    // Normalize percentages if passed as 0..100
    val normalizedTime = if (timePercentage > 1f) (timePercentage / 100f).coerceIn(0f, 1f) else timePercentage.coerceIn(0f, 1f)
    val normalizedKm = if (kmsPercentage > 1f) (kmsPercentage / 100f).coerceIn(0f, 1f) else kmsPercentage.coerceIn(0f, 1f)

    val animatedTime by animateFloatAsState(
        targetValue = normalizedTime,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "aero_runway_time"
    )
    val animatedKm by animateFloatAsState(
        targetValue = normalizedKm,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "aero_runway_km"
    )

    val isSafe = balance >= 0.0
    val kmBarColor = if (isSafe) CanvasKitTheme.colors.brandAccent else CanvasKitTheme.colors.error

    val statusColor = if (isSafe) CanvasKitTheme.colors.success else CanvasKitTheme.colors.error

    val diffPercentage = differencePercentage ?: run {
        val rawTimePercent = if (timePercentage > 1f) timePercentage else timePercentage * 100f
        val rawKmPercent = if (kmsPercentage > 1f) kmsPercentage else kmsPercentage * 100f
        rawTimePercent - rawKmPercent
    }

    CanvasKitCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("aero_runway_hero"),
        variant = CanvasKitCardVariant.Elevated,
        onClick = safeClick { onVehicleDetailClick() },
        header = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vehicle Identity & Detail Entry Point
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(6.dp))
                        .safeClickable(onClick = onVehicleDetailClick)
                        .padding(vertical = 2.dp, horizontal = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.brandAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = vehicleName.uppercase(),
                        style = CanvasKitTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = CanvasKitTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = stringResource(R.string.overview_view_vehicle_detail_acc),
                        tint = CanvasKitTheme.colors.textSecondary,
                        modifier = Modifier.size(11.dp)
                    )
                }

                // Right controls: Switcher pill (if >1 vehicle) + Status label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (availableVehicles.size > 1) {
                        Box {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CanvasKitTheme.colors.backgroundSecondary)
                                    .safeClickable(onClick = onToggleVehicleSwitcher)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.overview_hero_switch_vehicles_format,
                                        availableVehicles.size
                                    ),
                                    style = CanvasKitTheme.typography.labelSmall,
                                    color = CanvasKitTheme.colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            CanvasKitDropdownMenu(
                                expanded = showVehicleSwitcher,
                                onDismissRequest = onToggleVehicleSwitcher,
                            ) {
                                availableVehicles.forEach { vehicle ->
                                    CanvasKitDropdownMenuItem(
                                        text = vehicle.vehicleName,
                                        leadingIcon = {
                                            if (vehicle.isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = vehicle.vehicleName,
                                                    tint = CanvasKitTheme.colors.brandPrimary,
                                                )
                                            }
                                        },
                                        onClick = safeClick { onSwitchVehicle(vehicle.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main Balance Metric Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.overview_hero_current_balance),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary
                    )
                    Text(
                        text = "${if (balance >= 0) "+" else ""}${NumberFormatter.formatDistance(balance)} km",
                        style = CanvasKitTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = if (isSafe) CanvasKitTheme.colors.textPrimary else CanvasKitTheme.colors.error
                    )
                }

                // Delta Gap indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isSafe) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isSafe) {
                            stringResource(R.string.overview_hero_cushion_format, diffPercentage.absoluteValue)
                        } else {
                            stringResource(R.string.overview_hero_drift_format, diffPercentage.absoluteValue)
                        },
                        style = CanvasKitTheme.typography.labelLarge,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // The Runway Dual Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Time Bar (Passage of Contract Time)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.overview_hero_time_label),
                            style = CanvasKitTheme.typography.labelSmall,
                            color = CanvasKitTheme.colors.textSecondary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${(normalizedTime * 100).toInt()}%",
                            style = CanvasKitTheme.typography.labelSmall,
                            color = CanvasKitTheme.colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    RunwayProgressBar(
                        fraction = animatedTime,
                        fillColor = CanvasKitTheme.colors.textSecondary.copy(alpha = 0.5f),
                        backgroundColor = CanvasKitTheme.colors.borderSubtle
                    )
                }

                // Distance Bar (Consumed Distance)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.overview_hero_km_use_label),
                            style = CanvasKitTheme.typography.labelSmall,
                            color = CanvasKitTheme.colors.textSecondary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${(normalizedKm * 100).toInt()}%",
                            style = CanvasKitTheme.typography.labelSmall,
                            color = kmBarColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    RunwayProgressBar(
                        fraction = animatedKm,
                        fillColor = kmBarColor,
                        backgroundColor = CanvasKitTheme.colors.borderSubtle
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Footer info: Current Odometer & Days Remaining
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.overview_hero_odometer_format, NumberFormatter.formatDistance(currentOdometer)),
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary
                )

                if (daysRemaining > 0) {
                    Text(
                        text = stringResource(R.string.overview_hero_days_remaining_format, daysRemaining),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun RunwayProgressBar(
    fraction: Float,
    fillColor: androidx.compose.ui.graphics.Color,
    backgroundColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(fillColor)
        )
    }
}

@Preview(name = "AeroRunway - Safe State")
@Preview(name = "AeroRunway - Safe Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewAeroRunwaySafe() {
    CanvasKitTheme {
        AeroRunwayPacingBar(
            vehicleName = "Cupra Formentor",
            timePercentage = 0.42f,
            kmsPercentage = 0.34f,
            balance = 1450.0,
            currentOdometer = 24580.0,
            daysRemaining = 412,
            onVehicleDetailClick = {}
        )
    }
}

@Preview(name = "AeroRunway - Over Limit State")
@Composable
fun PreviewAeroRunwayOverLimit() {
    CanvasKitTheme {
        AeroRunwayPacingBar(
            vehicleName = "Cupra Formentor",
            timePercentage = 0.35f,
            kmsPercentage = 0.48f,
            balance = -650.0,
            currentOdometer = 28900.0,
            daysRemaining = 380,
            onVehicleDetailClick = {}
        )
    }
}
