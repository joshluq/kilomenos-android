package es.joshluq.kmsafe.feature.projection.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
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
import es.joshluq.kmsafe.core.ui.util.NumberFormatter
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.feature.projection.R

/**
 * Layer 4: Remedial Advisory Card (Prescripción Copilot).
 * Provides the exact daily pace prescription to end the contract with 0.00 € penalty.
 */
@Composable
fun RemedialAdvisoryCard(
    isPremium: Boolean,
    isOverLimit: Boolean,
    remedialDailyKm: Double?,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CanvasKitCard(
        modifier = modifier.fillMaxWidth(),
        variant = if (isPremium) CanvasKitCardVariant.Elevated else CanvasKitCardVariant.Outlined
    ) {
        Column(
            modifier = Modifier.padding(CanvasKitTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.sm)
        ) {
            if (!isOverLimit) {
                // Safe State: Reassuring message
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.success,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.projection_advisory_title),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = stringResource(R.string.projection_advisory_safe),
                    style = CanvasKitTheme.typography.bodyMedium,
                    color = CanvasKitTheme.colors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
            } else if (isPremium) {
                // Premium Over-limit: Exact Prescription
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.brandAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.projection_advisory_copilot_badge),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.brandAccent,
                        fontWeight = FontWeight.Black
                    )
                }

                if (remedialDailyKm != null && remedialDailyKm > 0.0) {
                    Text(
                        text = stringResource(R.string.projection_advisory_remedial_prescription),
                        style = CanvasKitTheme.typography.bodyMedium,
                        color = CanvasKitTheme.colors.textSecondary
                    )

                    Text(
                        text = stringResource(
                            R.string.projection_advisory_remedial_rate_highlight,
                            NumberFormatter.formatRate(remedialDailyKm)
                        ),
                        style = CanvasKitTheme.typography.headingMedium,
                        fontWeight = FontWeight.Black,
                        color = CanvasKitTheme.colors.brandAccent
                    )
                } else if (remedialDailyKm == 0.0) {
                    Text(
                        text = stringResource(R.string.projection_advisory_remedial_already_exceeded),
                        style = CanvasKitTheme.typography.bodyMedium,
                        color = CanvasKitTheme.colors.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Free Over-limit: Smart Teaser
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
                        text = stringResource(R.string.projection_advisory_free_teaser_title),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.brandAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = stringResource(R.string.projection_advisory_free_teaser_desc),
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
                        text = stringResource(R.string.projection_advisory_free_teaser_cta),
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
private fun PreviewRemedialAdvisoryCardPremiumOverLimit() {
    CanvasKitTheme {
        RemedialAdvisoryCard(
            isPremium = true,
            isOverLimit = true,
            remedialDailyKm = 36.8,
            onUpgradeClick = {}
        )
    }
}

@Preview
@Composable
private fun PreviewRemedialAdvisoryCardFree() {
    CanvasKitTheme {
        RemedialAdvisoryCard(
            isPremium = false,
            isOverLimit = true,
            remedialDailyKm = null,
            onUpgradeClick = {}
        )
    }
}
