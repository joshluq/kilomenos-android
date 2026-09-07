package es.joshluq.kmsafe.feature.projection.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.util.NumberFormatter
import es.joshluq.kmsafe.feature.projection.R

/**
 * Layer 3A: Pace Simulator Card.
 * Enables 1-Tap pace presets (-20%, -10%, Normal, +10%, +20%) and fine slider adjustment.
 */
@Composable
fun PaceSimulatorCard(
    realDailyAverage: Float,
    simulatedDailyKm: Float,
    isOverLimit: Boolean,
    onKmChanged: (Float) -> Unit,
    onPresetSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var localSliderValue by remember(simulatedDailyKm) { mutableFloatStateOf(simulatedDailyKm) }

    val accentColor = if (isOverLimit) CanvasKitTheme.colors.error else CanvasKitTheme.colors.brandAccent

    CanvasKitCard(
        modifier = modifier.fillMaxWidth(),
        header = {
            Text(
                text = stringResource(R.string.projection_sandbox_daily_pace_title),
                style = CanvasKitTheme.typography.labelLarge,
                color = CanvasKitTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(CanvasKitTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)
        ) {
            // Preset Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetChip(
                    label = stringResource(R.string.projection_sandbox_preset_minus_20),
                    isSelected = (simulatedDailyKm - (realDailyAverage * 0.8f)).let { kotlin.math.abs(it) < 0.5f },
                    onClick = { onPresetSelected(0.8f) },
                    modifier = Modifier.weight(1f)
                )
                PresetChip(
                    label = stringResource(R.string.projection_sandbox_preset_minus_10),
                    isSelected = (simulatedDailyKm - (realDailyAverage * 0.9f)).let { kotlin.math.abs(it) < 0.5f },
                    onClick = { onPresetSelected(0.9f) },
                    modifier = Modifier.weight(1f)
                )
                PresetChip(
                    label = stringResource(R.string.projection_sandbox_preset_normal),
                    isSelected = (simulatedDailyKm - realDailyAverage).let { kotlin.math.abs(it) < 0.5f },
                    onClick = { onPresetSelected(1.0f) },
                    modifier = Modifier.weight(1f)
                )
                PresetChip(
                    label = stringResource(R.string.projection_sandbox_preset_plus_10),
                    isSelected = (simulatedDailyKm - (realDailyAverage * 1.1f)).let { kotlin.math.abs(it) < 0.5f },
                    onClick = { onPresetSelected(1.1f) },
                    modifier = Modifier.weight(1f)
                )
                PresetChip(
                    label = stringResource(R.string.projection_sandbox_preset_plus_20),
                    isSelected = (simulatedDailyKm - (realDailyAverage * 1.2f)).let { kotlin.math.abs(it) < 0.5f },
                    onClick = { onPresetSelected(1.2f) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Slider & Values
            Column(verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.projection_analysis_actual_average,
                            NumberFormatter.formatRate(realDailyAverage.toDouble())
                        ),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary
                    )

                    Text(
                        text = stringResource(
                            R.string.projection_analysis_daily_km_slider,
                            NumberFormatter.formatRate(localSliderValue.toDouble())
                        ),
                        style = CanvasKitTheme.typography.bodyLarge,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }

                Slider(
                    value = localSliderValue,
                    onValueChange = {
                        localSliderValue = it
                        onKmChanged(it)
                    },
                    valueRange = 0f..150f,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = CanvasKitTheme.colors.borderSubtle
                    )
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) CanvasKitTheme.colors.brandAccent else CanvasKitTheme.colors.backgroundSecondary
    val textColor = if (isSelected) CanvasKitTheme.colors.onBrandAccent else CanvasKitTheme.colors.textPrimary
    val borderColor = if (isSelected) CanvasKitTheme.colors.brandAccent else CanvasKitTheme.colors.borderSubtle

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = CanvasKitTheme.typography.labelSmall,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
private fun PreviewPaceSimulatorCard() {
    CanvasKitTheme {
        PaceSimulatorCard(
            realDailyAverage = 42.5f,
            simulatedDailyKm = 42.5f,
            isOverLimit = false,
            onKmChanged = {},
            onPresetSelected = {}
        )
    }
}
