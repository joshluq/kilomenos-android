package es.joshluq.kmsafe.feature.projection.components

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.util.NumberFormatter
import es.joshluq.kmsafe.feature.projection.R
import kotlin.math.absoluteValue

/**
 * Layer 1: Runway Ceiling Bar (Aero Runway Pacing Bar).
 * Ultra-compact horizontal progress bar representing contract limit consumption.
 * Both Safe and Risk states maintain identical rounded corner geometry and padding.
 *
 * @param totalContractKms Total kilometers included in the renting/leasing contract.
 * @param startOdometer Initial odometer reading when the contract started.
 * @param projectedTotalKms Projected odometer reading at contract maturity.
 * @param finalBalance Projected balance in km (positive = cushion, negative = excess).
 * @param modifier Composable modifier.
 */
@Composable
fun RunwayCeilingBar(
    totalContractKms: Double,
    startOdometer: Double,
    projectedTotalKms: Double,
    finalBalance: Double,
    modifier: Modifier = Modifier
) {
    val isOverLimit = finalBalance < 0
    val contractLimitOdometer = startOdometer + totalContractKms
    val contractKmsConsumed = (projectedTotalKms - startOdometer).coerceAtLeast(0.0)

    // Ratio relative to contract limit
    val rawRatio = if (totalContractKms > 0) {
        (contractKmsConsumed / totalContractKms).toFloat()
    } else {
        1f
    }

    val animatedRatio by animateFloatAsState(
        targetValue = rawRatio,
        animationSpec = tween(durationMillis = 600),
        label = "RunwayCeilingRatioAnimation"
    )

    val brandColor = CanvasKitTheme.colors.brandAccent
    val successColor = CanvasKitTheme.colors.brandAccent
    val errorColor = CanvasKitTheme.colors.error
    val trackBackgroundColor = CanvasKitTheme.colors.borderSubtle.copy(alpha = 0.45f)
    val markerColor = CanvasKitTheme.colors.textPrimary
    val borderStrokeColor = CanvasKitTheme.colors.borderSubtle.copy(alpha = 0.6f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Track Canvas (Unified 8dp rounded outer container)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val cornerRadiusPx = 8.dp.toPx()
                val trackHeight = size.height
                val trackWidth = size.width

                // Unified rounded container path ensuring both Safe and Risk have smooth rounded corners
                val trackPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = 0f,
                            top = 0f,
                            right = trackWidth,
                            bottom = trackHeight,
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )
                    )
                }

                // 1. Draw contents clipped to the outer rounded shape
                clipPath(trackPath) {
                    if (!isOverLimit) {
                        // SAFE MODE: Full width represents 100% of the contractual ceiling
                        // Base track background
                        drawRect(
                            color = trackBackgroundColor,
                            topLeft = Offset.Zero,
                            size = Size(trackWidth, trackHeight)
                        )

                        // Progress fill
                        val progressWidth = (animatedRatio.coerceIn(0f, 1f) * trackWidth)
                        if (progressWidth > 0f) {
                            drawRoundRect(
                                color = successColor,
                                topLeft = Offset.Zero,
                                size = Size(progressWidth, trackHeight),
                                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                            )
                        }
                    } else {
                        // RISK MODE: 80% width is 100% contract ceiling, 20% width is danger overflow zone
                        val ceilingX = trackWidth * 0.80f

                        // Base track (contract portion background)
                        drawRect(
                            color = trackBackgroundColor,
                            topLeft = Offset.Zero,
                            size = Size(ceilingX, trackHeight)
                        )

                        // Base track (overflow zone background)
                        drawRect(
                            color = errorColor.copy(alpha = 0.15f),
                            topLeft = Offset(ceilingX, 0f),
                            size = Size(trackWidth - ceilingX, trackHeight)
                        )

                        // Contract portion full progress fill (up to ceilingX)
                        drawRect(
                            color = brandColor,
                            topLeft = Offset.Zero,
                            size = Size(ceilingX, trackHeight)
                        )

                        // Overflow fill beyond ceilingX
                        val excessFraction = ((animatedRatio - 1f) / 0.35f).coerceIn(0f, 1f)
                        val overflowWidth = (trackWidth - ceilingX) * excessFraction
                        if (overflowWidth > 0f) {
                            drawRoundRect(
                                color = errorColor,
                                topLeft = Offset(ceilingX, 0f),
                                size = Size(overflowWidth, trackHeight),
                                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                            )
                        }
                    }
                }

                // 2. Subtle outer border for high-end definition
                drawRoundRect(
                    color = borderStrokeColor,
                    topLeft = Offset.Zero,
                    size = Size(trackWidth, trackHeight),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    style = Stroke(width = 1.dp.toPx())
                )

                // 3. Milestone Marker (drawn on top, extending slightly beyond track height)
                if (!isOverLimit) {
                    // Safe mode: Milestone marker at the 100% right boundary
                    val markerX = trackWidth - 2.dp.toPx()
                    drawLine(
                        color = markerColor,
                        start = Offset(markerX, -2.dp.toPx()),
                        end = Offset(markerX, trackHeight + 2.dp.toPx()),
                        strokeWidth = 3.dp.toPx()
                    )
                } else {
                    // Risk mode: Milestone marker at the 80% ceiling line
                    val ceilingX = trackWidth * 0.80f
                    drawLine(
                        color = markerColor,
                        start = Offset(ceilingX, -3.dp.toPx()),
                        end = Offset(ceilingX, trackHeight + 3.dp.toPx()),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
        }

        // Subtitle Milestone / Status Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val startText = if (startOdometer > 0) {
                "${NumberFormatter.formatDistance(startOdometer)} km"
            } else {
                "0 km"
            }
            Text(
                text = startText,
                style = CanvasKitTheme.typography.labelSmall,
                color = CanvasKitTheme.colors.textSecondary
            )

            if (!isOverLimit) {
                Text(
                    text = stringResource(
                        R.string.projection_ceiling_bar_cushion_label,
                        NumberFormatter.formatDistance(finalBalance)
                    ),
                    style = CanvasKitTheme.typography.labelSmall,
                    color = successColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(
                        R.string.projection_ceiling_bar_ceiling_label,
                        NumberFormatter.formatDistance(contractLimitOdometer)
                    ),
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.projection_ceiling_bar_ceiling_label,
                        NumberFormatter.formatDistance(contractLimitOdometer)
                    ),
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(
                        R.string.projection_ceiling_bar_excess_label,
                        NumberFormatter.formatDistance(finalBalance.absoluteValue)
                    ),
                    style = CanvasKitTheme.typography.labelSmall,
                    color = errorColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
private fun PreviewRunwayCeilingBarSafe() {
    CanvasKitTheme {
        RunwayCeilingBar(
            totalContractKms = 45000.0,
            startOdometer = 10000.0,
            projectedTotalKms = 53500.0,
            finalBalance = 1500.0,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
private fun PreviewRunwayCeilingBarRisk() {
    CanvasKitTheme {
        RunwayCeilingBar(
            totalContractKms = 45000.0,
            startOdometer = 10000.0,
            projectedTotalKms = 57450.0,
            finalBalance = -2450.0,
            modifier = Modifier.padding(16.dp)
        )
    }
}
