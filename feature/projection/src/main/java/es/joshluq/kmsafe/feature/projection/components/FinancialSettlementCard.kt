package es.joshluq.kmsafe.feature.projection.components

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
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
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.cards.CanvasKitCardVariant
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.feature.projection.R
import es.joshluq.kmsafe.core.ui.R as CoreR
import androidx.compose.ui.platform.LocalLocale

/**
 * Layer 2: Financial Settlement & Breakdown Card.
 * Transparently displays gross excess, courtesy buffer (exempt), net billable kms, applied excess rate,
 * and whether values originate from verified contract terms or market default benchmarks.
 */
@Composable
fun FinancialSettlementCard(
    isOverLimit: Boolean,
    grossExcessKms: Double,
    courtesyMarginKms: Double,
    billableExcessKms: Double,
    ratePerKm: Float,
    estimatedPenalty: Double,
    courtesySavingsAmount: Double,
    isUsingDefaultPrice: Boolean,
    isUsingDefaultCourtesyMargin: Boolean,
    onConfigureContractClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUsingDefaults = isUsingDefaultPrice || isUsingDefaultCourtesyMargin
    val isProtectedByCourtesy = isOverLimit && billableExcessKms <= 0.0 && grossExcessKms > 0.0

    val locale = LocalLocale.current.platformLocale

    val formattedRate = String.format(locale, "%.2f €", ratePerKm)
    val formattedSavings = String.format(locale, "%.2f €", courtesySavingsAmount)

    CanvasKitCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 250)),
        variant = CanvasKitCardVariant.Outlined
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)
        ) {
            // Header: Section Icon + Title + Origin Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.brandAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.projection_settlement_card_title),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

            }

            // Notice Banner when using Market Defaults
            if (isUsingDefaults) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = CanvasKitTheme.colors.brandAccent.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(CanvasKitTheme.spacing.sm)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = CanvasKitTheme.colors.brandAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.projection_settlement_defaults_notice),
                                style = CanvasKitTheme.typography.labelSmall,
                                color = CanvasKitTheme.colors.textSecondary
                            )
                        }

                        CanvasKitButton(
                            text = stringResource(R.string.projection_settlement_configure_cta),
                            onClick = safeClick { onConfigureContractClick() },
                            variant = CanvasKitButtonVariant.Secondary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Settlement Mathematical Breakdown Feed
            Column(
                verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.xs)
            ) {
                // Row 1: Gross Excess Kms
                SettlementRow(
                    label = stringResource(R.string.projection_settlement_gross_excess_label),
                    value = if (isOverLimit) {
                        "+ ${grossExcessKms.toInt()} km"
                    } else {
                        "0 km"
                    },
                    valueColor = if (isOverLimit) CanvasKitTheme.colors.error else CanvasKitTheme.colors.textPrimary
                )

                // Row 2: Courtesy Margin (Exempt Cushion)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.projection_settlement_courtesy_margin_label),
                            style = CanvasKitTheme.typography.bodyMedium,
                            color = CanvasKitTheme.colors.textSecondary
                        )
                        if (courtesySavingsAmount > 0.0) {
                            Box(
                                modifier = Modifier
                                    .background(CanvasKitTheme.colors.success.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.projection_settlement_courtesy_savings_chip, formattedSavings),
                                    style = CanvasKitTheme.typography.labelSmall,
                                    color = CanvasKitTheme.colors.success,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "- ${courtesyMarginKms.toInt()} km",
                        style = CanvasKitTheme.typography.bodyMedium,
                        color = CanvasKitTheme.colors.success,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = CanvasKitTheme.colors.borderSubtle
                )

                // Row 3: Net Billable Excess Kms
                SettlementRow(
                    label = stringResource(R.string.projection_settlement_billable_excess_label),
                    value = "${billableExcessKms.toInt()} km",
                    valueColor = if (billableExcessKms > 0) CanvasKitTheme.colors.error else CanvasKitTheme.colors.textPrimary,
                    isBold = true
                )

                // Row 4: Unit Rate per Km
                SettlementRow(
                    label = stringResource(R.string.projection_settlement_rate_applied_label),
                    value = stringResource(R.string.projection_settlement_rate_format, formattedRate),
                    valueColor = CanvasKitTheme.colors.textPrimary
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = CanvasKitTheme.colors.borderSubtle
                )

                // Row 5: Total Estimated Settlement
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.projection_settlement_net_penalty_label),
                        style = CanvasKitTheme.typography.labelLarge,
                        color = CanvasKitTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (isOverLimit && billableExcessKms > 0.0) {
                            stringResource(CoreR.string.common_currency_format, estimatedPenalty)
                        } else {
                            stringResource(R.string.projection_sentinel_safe_penalty)
                        },
                        style = CanvasKitTheme.typography.headingMedium,
                        color = if (isOverLimit && billableExcessKms > 0.0) {
                            CanvasKitTheme.colors.error
                        } else {
                            CanvasKitTheme.colors.success
                        },
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.End
                    )
                }

                // Protected by Courtesy Notice Banner
                if (isProtectedByCourtesy) {
                    val cushionRemaining = (courtesyMarginKms - grossExcessKms).toInt().coerceAtLeast(0)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CanvasKitTheme.colors.success.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CanvasKitTheme.colors.success,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.projection_settlement_protected_notice, cushionRemaining),
                            style = CanvasKitTheme.typography.labelSmall,
                            color = CanvasKitTheme.colors.success,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettlementRow(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    isBold: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = CanvasKitTheme.typography.bodyMedium,
            color = CanvasKitTheme.colors.textSecondary,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = CanvasKitTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
private fun PreviewFinancialSettlementCardContractVerified() {
    CanvasKitTheme {
        FinancialSettlementCard(
            isOverLimit = true,
            grossExcessKms = 2500.0,
            courtesyMarginKms = 1000.0,
            billableExcessKms = 1500.0,
            ratePerKm = 0.05f,
            estimatedPenalty = 75.0,
            courtesySavingsAmount = 50.0,
            isUsingDefaultPrice = false,
            isUsingDefaultCourtesyMargin = false,
            onConfigureContractClick = {}
        )
    }
}

@Preview
@Composable
private fun PreviewFinancialSettlementCardProtectedByCourtesy() {
    CanvasKitTheme {
        FinancialSettlementCard(
            isOverLimit = true,
            grossExcessKms = 350.0,
            courtesyMarginKms = 500.0,
            billableExcessKms = 0.0,
            ratePerKm = 0.06f,
            estimatedPenalty = 0.0,
            courtesySavingsAmount = 21.0,
            isUsingDefaultPrice = true,
            isUsingDefaultCourtesyMargin = true,
            onConfigureContractClick = {}
        )
    }
}

@Preview
@Composable
private fun PreviewFinancialSettlementCardMarketDefaults() {
    CanvasKitTheme {
        FinancialSettlementCard(
            isOverLimit = true,
            grossExcessKms = 2000.0,
            courtesyMarginKms = 500.0,
            billableExcessKms = 1500.0,
            ratePerKm = 0.06f,
            estimatedPenalty = 90.0,
            courtesySavingsAmount = 30.0,
            isUsingDefaultPrice = true,
            isUsingDefaultCourtesyMargin = true,
            onConfigureContractClick = {}
        )
    }
}
