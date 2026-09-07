package es.joshluq.kmsafe.feature.projection.components

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.cards.CanvasKitCardVariant
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.util.DateUtils
import es.joshluq.kmsafe.core.ui.util.NumberFormatter
import es.joshluq.kmsafe.feature.projection.R
import kotlin.math.absoluteValue
import es.joshluq.kmsafe.core.ui.R as CoreR

/**
 * Layer 1: Financial Sentinel Hero Card.
 * Displays the contractual end status, surplus/deficit mileage balance, financial penalty in Euros,
 * and the visual trajectory gauge.
 */
@Composable
fun ProjectionSentinelCard(
    endDateMillis: Long,
    totalContractKms: Double,
    startOdometer: Double,
    projectedTotalKms: Double,
    finalBalance: Double,
    estimatedPenalty: Double,
    modifier: Modifier = Modifier
) {
    val isOverLimit = finalBalance < 0
    val accentColor = if (isOverLimit) CanvasKitTheme.colors.error else CanvasKitTheme.colors.success
    val badgeBgColor = if (isOverLimit) CanvasKitTheme.colors.error.copy(alpha = 0.12f) else CanvasKitTheme.colors.success.copy(alpha = 0.12f)
    val badgeText = if (isOverLimit) {
        stringResource(R.string.projection_sentinel_badge_risk)
    } else {
        stringResource(R.string.projection_sentinel_badge_safe)
    }

    val maturityFormatted = if (endDateMillis > 0L) DateUtils.formatShortDate(endDateMillis) else ""
    val contractLimitOdometer = startOdometer + totalContractKms

    CanvasKitCard(
        modifier = modifier.fillMaxWidth(),
        variant = CanvasKitCardVariant.Elevated
    ) {
        Column(
            modifier = Modifier.padding(CanvasKitTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Maturity date & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.projection_sentinel_maturity_date, maturityFormatted),
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier
                        .background(badgeBgColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isOverLimit) Icons.Default.Warning else Icons.Default.Shield,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = badgeText,
                        style = CanvasKitTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Dual Primary Metrics: Balance Km (Left) & Euro Penalty (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left: Balance Kms
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isOverLimit) {
                            stringResource(R.string.projection_sentinel_excess_title)
                        } else {
                            stringResource(R.string.projection_sentinel_surplus_title)
                        },
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isOverLimit) {
                            stringResource(R.string.projection_sentinel_deficit_kms, finalBalance.absoluteValue.toInt())
                        } else {
                            stringResource(R.string.projection_sentinel_surplus_kms, finalBalance.toInt())
                        },
                        style = CanvasKitTheme.typography.headingLarge,
                        color = accentColor,
                        fontWeight = FontWeight.Black
                    )
                }

                // Right: Euro Penalty
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = stringResource(R.string.projection_sentinel_penalty_title),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isOverLimit) {
                            stringResource(CoreR.string.common_currency_format, estimatedPenalty)
                        } else {
                            stringResource(R.string.projection_sentinel_safe_penalty)
                        },
                        style = CanvasKitTheme.typography.headingLarge,
                        color = accentColor,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.End
                    )
                }
            }

            // Runway Ceiling Bar (Aero Horizon)
            RunwayCeilingBar(
                totalContractKms = totalContractKms,
                startOdometer = startOdometer,
                projectedTotalKms = projectedTotalKms,
                finalBalance = finalBalance,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Bottom Odometer Summary
            Text(
                text = stringResource(
                    R.string.projection_sentinel_odometer_reading,
                    NumberFormatter.formatDistance(projectedTotalKms),
                    NumberFormatter.formatDistance(contractLimitOdometer)
                ),
                style = CanvasKitTheme.typography.labelSmall,
                color = CanvasKitTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
private fun PreviewProjectionSentinelCardSafe() {
    CanvasKitTheme {
        ProjectionSentinelCard(
            endDateMillis = System.currentTimeMillis() + (300L * 24 * 3600 * 1000),
            totalContractKms = 45000.0,
            startOdometer = 10000.0,
            projectedTotalKms = 53500.0,
            finalBalance = 1500.0,
            estimatedPenalty = 0.0
        )
    }
}

@Preview
@Composable
private fun PreviewProjectionSentinelCardRisk() {
    CanvasKitTheme {
        ProjectionSentinelCard(
            endDateMillis = System.currentTimeMillis() + (300L * 24 * 3600 * 1000),
            totalContractKms = 45000.0,
            startOdometer = 10000.0,
            projectedTotalKms = 57450.0,
            finalBalance = -2450.0,
            estimatedPenalty = 196.0
        )
    }
}
