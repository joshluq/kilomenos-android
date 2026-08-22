package es.joshluq.kmsafe.feature.expenses.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.util.toTextProvider
import es.joshluq.kmsafe.domain.model.PriceTrend
import es.joshluq.kmsafe.domain.model.StationPriceVolatility
import es.joshluq.kmsafe.feature.expenses.R
import androidx.compose.ui.platform.LocalLocale
import es.joshluq.canvaskit.components.buttons.CanvasKitButton

@Composable
fun StationVolatilityCard(
    modifier: Modifier = Modifier,
    volatility: StationPriceVolatility,
    onDismiss: () -> Unit,
    onViewDetail: (String) -> Unit = {},
) {
    val locale = LocalLocale.current.platformLocale
    CanvasKitCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.expenses_volatility_title, volatility.fuelType.toTextProvider().asString()),
                    style = CanvasKitTheme.typography.headingMedium,
                    fontWeight = FontWeight.Bold,
                    color = CanvasKitTheme.colors.textPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.expenses_volatility_current_price),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary
                    )
                    Text(
                        text = String.format(locale, "%.3f €/%s", volatility.currentPrice, volatility.fuelType.unitOfMeasure),
                        style = CanvasKitTheme.typography.headingLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = CanvasKitTheme.colors.brandAccent
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.expenses_volatility_historical_avg),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary
                    )
                    Text(
                        text = String.format(locale, "%.3f €/%s", volatility.historicalAveragePrice, volatility.fuelType.unitOfMeasure),
                        style = CanvasKitTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = CanvasKitTheme.colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.sm))

            val (trendText, trendColor, trendIcon) = when (volatility.priceTrend) {
                PriceTrend.CHEAPER -> Triple(
                    stringResource(R.string.expenses_volatility_cheaper),
                    CanvasKitTheme.colors.success,
                    Icons.Default.ArrowDownward
                )
                PriceTrend.EXPENSIVE -> Triple(
                    stringResource(R.string.expenses_volatility_expensive),
                    CanvasKitTheme.colors.error,
                    Icons.Default.ArrowUpward
                )
                PriceTrend.AVERAGE -> Triple(
                    stringResource(R.string.expenses_volatility_average),
                    CanvasKitTheme.colors.textSecondary,
                    Icons.Default.Remove
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = trendIcon,
                    contentDescription = null,
                    tint = trendColor
                )
                Text(
                    text = trendText,
                    style = CanvasKitTheme.typography.bodyMedium,
                    color = trendColor,
                    fontWeight = FontWeight.Medium
                )
            }

            if (volatility.priceHistory.size > 1) {
                PriceLineChart(
                    history = volatility.priceHistory,
                    minPrice = volatility.minRecordedPrice,
                    maxPrice = volatility.maxRecordedPrice,
                    avgPrice = volatility.historicalAveragePrice
                )
            }

            CanvasKitButton(
                text = stringResource(R.string.stations_action_view_detail),
                variant = es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant.Ghost,
                onClick = { onViewDetail(volatility.stationId) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
