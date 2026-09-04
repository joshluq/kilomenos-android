package es.joshluq.kmsafe.feature.expenses.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonSize
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.cards.CanvasKitCardVariant
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.domain.model.StationRadarItem
import es.joshluq.kmsafe.feature.expenses.R
import kotlin.math.abs

/**
 * Layer 2: Contextual Decision Radar.
 * Displays "My Stations" price comparison carousel with delta badges and direct "Estoy aquí" shortcut.
 */
@Composable
fun StationRadarCarousel(
    modifier: Modifier = Modifier,
    isLocked: Boolean,
    items: List<StationRadarItem>,
    onUpgradeClick: () -> Unit
) {
    if (isLocked) {
        CanvasKitCard(
            modifier = modifier.fillMaxWidth(),
            variant = CanvasKitCardVariant.Outlined
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(CanvasKitTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.xs)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.brandAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.expenses_radar_locked_title),
                        style = CanvasKitTheme.typography.headingMedium,
                        fontWeight = FontWeight.Bold,
                        color = CanvasKitTheme.colors.textPrimary
                    )
                }
                Text(
                    text = stringResource(R.string.expenses_radar_locked_desc),
                    style = CanvasKitTheme.typography.bodyMedium,
                    color = CanvasKitTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.xxs))
                CanvasKitButton(
                    text = stringResource(R.string.expenses_radar_unlock_action),
                    variant = CanvasKitButtonVariant.Primary,
                    size = CanvasKitButtonSize.Small,
                    onClick = safeClick { onUpgradeClick() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }

    if (items.isEmpty()) return

    val locale = LocalLocale.current.platformLocale

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.expenses_radar_subtitle).uppercase(),
                style = CanvasKitTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = CanvasKitTheme.colors.textSecondary
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.sm),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(items, key = { it.station.id }) { item ->
                StationRadarCard(
                    item = item,
                    locale = locale
                )
            }
        }
    }
}

@Composable
private fun StationRadarCard(
    item: StationRadarItem,
    locale: java.util.Locale,
    modifier: Modifier = Modifier
) {
    val unit = item.fuelType.unitOfMeasure
    CanvasKitCard(
        modifier = modifier.width(180.dp),
        variant = CanvasKitCardVariant.Elevated
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.xxs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.station.name,
                    style = CanvasKitTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CanvasKitTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = String.format(locale, "%.3f €/%s", item.lastRecordedPrice, unit),
                style = CanvasKitTheme.typography.headingMedium,
                fontWeight = FontWeight.ExtraBold,
                color = CanvasKitTheme.colors.brandAccent
            )

            // Delta Badge
            val deltaText: String
            val deltaColor = when {
                item.isOpportunity -> {
                    deltaText = stringResource(R.string.expenses_radar_opportunity, abs(item.priceDelta))
                    CanvasKitTheme.colors.success
                }
                item.priceDelta > 0.01 -> {
                    deltaText = stringResource(R.string.expenses_radar_expensive, item.priceDelta)
                    CanvasKitTheme.colors.error
                }
                else -> {
                    deltaText = stringResource(R.string.expenses_radar_average)
                    CanvasKitTheme.colors.textSecondary
                }
            }

            Text(
                text = deltaText,
                style = CanvasKitTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = deltaColor
            )
        }
    }
}
