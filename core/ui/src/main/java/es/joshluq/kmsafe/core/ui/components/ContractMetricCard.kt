package es.joshluq.kmsafe.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme

/**
 * Small card to display a specific contract metric (e.g., "48 months").
 */
@Composable
fun ContractMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    CanvasKitCard(
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CanvasKitTheme.colors.brandAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = label,
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary
                )
                Text(
                    text = value,
                    style = CanvasKitTheme.typography.bodyMedium,
                    color = CanvasKitTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
