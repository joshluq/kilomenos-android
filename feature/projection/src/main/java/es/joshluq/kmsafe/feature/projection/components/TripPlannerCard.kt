package es.joshluq.kmsafe.feature.projection.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.feedback.CanvasKitDialog
import es.joshluq.canvaskit.components.feedback.CanvasKitDialogContent
import es.joshluq.canvaskit.components.inputs.CanvasKitTextField
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.util.NumberFormatter
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.domain.model.PlannedTrip
import es.joshluq.kmsafe.feature.projection.R

/**
 * Layer 3B: Trip Planner Card (Multi-trip Stacking).
 * Allows adding, stacking, and removing planned vacations and getaways,
 * with quick presets or a custom modal dialog for exact title & kilometer inputs.
 */
@Composable
fun TripPlannerCard(
    isPremium: Boolean,
    plannedTrips: List<PlannedTrip>,
    isOverLimit: Boolean,
    onAddTrip: (title: String, distanceKms: Int) -> Unit,
    onRemoveTrip: (tripId: String) -> Unit,
    onCustomTripChanged: (distanceKms: Int) -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isOverLimit) CanvasKitTheme.colors.error else CanvasKitTheme.colors.brandAccent
    var showCustomTripDialog by remember { mutableStateOf(false) }
    var customTripTitle by remember { mutableStateOf("") }
    var customTripDistance by remember { mutableStateOf("") }

    CanvasKitCard(
        modifier = modifier.fillMaxWidth(),
        header = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.projection_sandbox_trip_planner_title),
                    style = CanvasKitTheme.typography.labelLarge,
                    color = CanvasKitTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                if (plannedTrips.isNotEmpty()) {
                    val totalKm = plannedTrips.sumOf { it.distanceKms }
                    Text(
                        text = "+ ${NumberFormatter.formatDistance(totalKm.toDouble())} km",
                        style = CanvasKitTheme.typography.labelLarge,
                        color = accentColor,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(CanvasKitTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)
        ) {
            // Quick Add Chips: Escapada (350 km), Verano (1.500 km), Personalizado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TripPresetChip(
                    label = stringResource(R.string.projection_sandbox_preset_getaway),
                    onClick = {
                        if (!isPremium && plannedTrips.isNotEmpty()) {
                            onUpgradeClick()
                        } else {
                            onAddTrip("Escapada", 350)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                TripPresetChip(
                    label = stringResource(R.string.projection_sandbox_preset_vacation),
                    onClick = {
                        if (!isPremium && plannedTrips.isNotEmpty()) {
                            onUpgradeClick()
                        } else {
                            onAddTrip("Vacaciones", 1500)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                TripPresetChip(
                    label = stringResource(R.string.projection_sandbox_preset_custom),
                    onClick = {
                        if (!isPremium && plannedTrips.isNotEmpty()) {
                            onUpgradeClick()
                        } else {
                            customTripTitle = ""
                            customTripDistance = ""
                            showCustomTripDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Stacking List / Free Single Trip
            if (isPremium) {
                if (plannedTrips.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.projection_sandbox_trips_stacked_title, plannedTrips.size),
                            style = CanvasKitTheme.typography.labelSmall,
                            color = CanvasKitTheme.colors.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )

                        plannedTrips.forEach { trip ->
                            PlannedTripRow(
                                trip = trip,
                                onRemove = { onRemoveTrip(trip.id) }
                            )
                        }
                    }
                }
            } else {
                // Free Mode: Slider for 1 trip + Upgrade Teaser
                val currentFreeKm = plannedTrips.firstOrNull()?.distanceKms ?: 0
                Column(verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.xs)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.projection_analysis_planned_trip_increment, currentFreeKm),
                            style = CanvasKitTheme.typography.bodyLarge,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = currentFreeKm.toFloat(),
                        onValueChange = { onCustomTripChanged(it.toInt()) },
                        valueRange = 0f..5000f,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = CanvasKitTheme.colors.borderSubtle
                        )
                    )

                    if (plannedTrips.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CanvasKitTheme.colors.backgroundSecondary, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CanvasKitTheme.colors.brandAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.projection_sandbox_free_trip_limit_notice),
                                    style = CanvasKitTheme.typography.labelSmall,
                                    color = CanvasKitTheme.colors.textSecondary
                                )
                            }
                            CanvasKitButton(
                                onClick = safeClick { onUpgradeClick() },
                                variant = CanvasKitButtonVariant.Primary
                            ) { contentColor ->
                                Text(
                                    text = "PRO",
                                    style = CanvasKitTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog for Custom Trip Creation
    if (showCustomTripDialog) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        CanvasKitDialog(
            onDismissRequest = { showCustomTripDialog = false }
        ) {
            CanvasKitDialogContent(
                title = {
                    Text(
                        text = stringResource(R.string.projection_custom_trip_dialog_title),
                        style = CanvasKitTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CanvasKitTextField(
                            label = stringResource(R.string.projection_custom_trip_name_label),
                            value = customTripTitle,
                            onValueChange = { customTripTitle = it },
                            placeholder = stringResource(R.string.projection_custom_trip_name_placeholder),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Next) }
                            )
                        )

                        CanvasKitTextField(
                            label = stringResource(R.string.projection_custom_trip_distance_label),
                            value = customTripDistance,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) {
                                    customTripDistance = input
                                }
                            },
                            placeholder = stringResource(R.string.projection_custom_trip_distance_placeholder),
                            suffix = "km",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            )
                        )
                    }
                },
                buttons = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CanvasKitButton(
                            text = stringResource(R.string.projection_custom_trip_cancel_button),
                            variant = CanvasKitButtonVariant.Secondary,
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                showCustomTripDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        val distanceInt = customTripDistance.toIntOrNull() ?: 0
                        CanvasKitButton(
                            text = stringResource(R.string.projection_custom_trip_confirm_button),
                            variant = CanvasKitButtonVariant.Primary,
                            enabled = distanceInt > 0,
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                val title = customTripTitle.trim().ifBlank { "Escapada" }
                                onAddTrip(title, distanceInt)
                                showCustomTripDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun TripPresetChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CanvasKitTheme.colors.backgroundSecondary)
            .border(1.dp, CanvasKitTheme.colors.borderSubtle, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = CanvasKitTheme.colors.brandAccent,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = CanvasKitTheme.typography.labelSmall,
            color = CanvasKitTheme.colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlannedTripRow(
    trip: PlannedTrip,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CanvasKitTheme.colors.backgroundSecondary)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = trip.title,
                style = CanvasKitTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = CanvasKitTheme.colors.textPrimary
            )
            Text(
                text = "+ ${NumberFormatter.formatDistance(trip.distanceKms.toDouble())} km",
                style = CanvasKitTheme.typography.labelSmall,
                color = CanvasKitTheme.colors.brandAccent
            )
        }

        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.projection_sandbox_remove_trip_content_description, trip.title),
                tint = CanvasKitTheme.colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
private fun PreviewTripPlannerCardPremium() {
    CanvasKitTheme {
        TripPlannerCard(
            isPremium = true,
            plannedTrips = listOf(
                PlannedTrip(title = "Escapada Montaña", distanceKms = 350),
                PlannedTrip(title = "Vacaciones Verano", distanceKms = 1500)
            ),
            isOverLimit = false,
            onAddTrip = { _, _ -> },
            onRemoveTrip = {},
            onCustomTripChanged = {},
            onUpgradeClick = {}
        )
    }
}
