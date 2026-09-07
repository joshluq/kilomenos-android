package es.joshluq.kmsafe.feature.projection.components

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.cards.CanvasKitCardVariant
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.util.DateUtils
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.feature.projection.R

/**
 * Layer 2: Runway Timeline Card (Radar de Agotamiento).
 * Displays the exact exhaustion date in calendar for Premium users, or an ethical conversion teaser for Free users.
 * Symmetrical 3-tier structure in both Safe and Risk states with smooth animated transitions.
 */
@Composable
fun RunwayTimelineCard(
    isPremium: Boolean,
    isOverLimit: Boolean,
    exhaustionDateMillis: Long?,
    monthsAheadOrBehind: Int,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CanvasKitCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 250)),
        variant = CanvasKitCardVariant.Outlined
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.sm)
        ) {
            if (isPremium) {
                // Header: Icon + Section Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isOverLimit) Icons.Default.DateRange else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isOverLimit) CanvasKitTheme.colors.error else CanvasKitTheme.colors.success,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.projection_runway_title),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isOverLimit && exhaustionDateMillis != null) {
                    val dateFormatted = DateUtils.formatMonthYear(exhaustionDateMillis)
                    Text(
                        text = stringResource(R.string.projection_runway_collision_warning, dateFormatted),
                        style = CanvasKitTheme.typography.headingMedium,
                        fontWeight = FontWeight.Black,
                        color = CanvasKitTheme.colors.error
                    )
                    if (monthsAheadOrBehind > 0) {
                        Text(
                            text = stringResource(R.string.projection_runway_collision_months_early, monthsAheadOrBehind),
                            style = CanvasKitTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CanvasKitTheme.colors.error
                        )
                    }
                } else {
                    // Safe State Mirror: Hero Headline + Contextual reassurance
                    Text(
                        text = stringResource(R.string.projection_runway_safe_headline),
                        style = CanvasKitTheme.typography.headingMedium,
                        fontWeight = FontWeight.Black,
                        color = CanvasKitTheme.colors.success
                    )
                    Text(
                        text = stringResource(R.string.projection_runway_safe_description),
                        style = CanvasKitTheme.typography.bodyMedium,
                        color = CanvasKitTheme.colors.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                // Free Teaser Content: Ethical Conversion Hook
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.brandAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.projection_runway_free_teaser_title),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.brandAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = stringResource(R.string.projection_runway_free_teaser_desc),
                    style = CanvasKitTheme.typography.bodyMedium,
                    color = CanvasKitTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                CanvasKitButton(
                    onClick = safeClick { onUpgradeClick() },
                    variant = CanvasKitButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth()
                ) { contentColor ->
                    Text(
                        text = stringResource(R.string.projection_runway_free_teaser_cta),
                        style = CanvasKitTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
private fun PreviewRunwayTimelineCardPremiumSafeLimit() {
    CanvasKitTheme {
        RunwayTimelineCard(
            isPremium = true,
            isOverLimit = false,
            exhaustionDateMillis = System.currentTimeMillis() + (180L * 24 * 3600 * 1000),
            monthsAheadOrBehind = 8,
            onUpgradeClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
private fun PreviewRunwayTimelineCardPremiumOverLimit() {
    CanvasKitTheme {
        RunwayTimelineCard(
            isPremium = true,
            isOverLimit = true,
            exhaustionDateMillis = System.currentTimeMillis() + (180L * 24 * 3600 * 1000),
            monthsAheadOrBehind = 8,
            onUpgradeClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
private fun PreviewRunwayTimelineCardFree() {
    CanvasKitTheme {
        RunwayTimelineCard(
            isPremium = false,
            isOverLimit = true,
            exhaustionDateMillis = null,
            monthsAheadOrBehind = 0,
            onUpgradeClick = {}
        )
    }
}
